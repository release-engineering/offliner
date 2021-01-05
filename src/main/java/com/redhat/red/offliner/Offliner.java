/*
 * Copyright (C) 2015 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.red.offliner;

import com.redhat.red.offliner.alist.PlaintextArtifactListReader;
import com.redhat.red.offliner.model.DownloadResult;
import com.redhat.red.offliner.util.UrlUtils;
import com.redhat.red.offliner.alist.ArtifactListReader;
import com.redhat.red.offliner.alist.FoloReportArtifactListReader;
import com.redhat.red.offliner.alist.PomArtifactListReader;
import com.redhat.red.offliner.model.ArtifactList;
import io.honeycomb.beeline.DefaultBeeline;
import io.honeycomb.beeline.tracing.Span;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.redhat.red.offliner.OfflinerUtils.*;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Entry point to Offliner, this class is responsible for orchestrating the entire process.
 */
public class Offliner
    implements AutoCloseable
{
    public static final String SEPARATING_LINE = "---------------------------------------------------------------";

    public static final String SHA_SUFFIX = ".sha1";

    public static final String MD5_SUFFIX = ".md5";

    public static final double NANOS_PER_MILLISECOND = 1E6;

    public static final String HONEYCOMB_DATASET = "honeycomb.dataset";

    public static final String HONEYCOMB_SERVICE_NAME = "honeycomb.service.name";

    public static final String HONEYCOMB_WRITE_KEY = "honeycomb.write.key";

    private static final int CONNECTION_REQUEST_TIMEOUT = 30 * 1000; // 30s

    private static final int CONNECTION_TIMEOUT = 60 * 1000; // 60s

    private static final int SOCKET_TIMEOUT = 10 * 60 * 1000; // 10 minutes

    private String proxyHost;

    private int proxyPort = 8080;

    private CloseableHttpClient client;

    private ExecutorService executorService;

    private List<ArtifactListReader> artifactListReaders;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public Offliner( final OfflinerConfig config )
    {
        int threads = config.getThreads();
        executorService = Executors.newFixedThreadPool( threads, ( final Runnable r ) -> {
            //        executorService = Executors.newCachedThreadPool( ( final Runnable r ) -> {
            final Thread t = new Thread( r );
            t.setDaemon( true );

            return t;
        } );

        final PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager();
        ccm.setMaxTotal( config.getConnections() );

        RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout( CONNECTION_REQUEST_TIMEOUT )
                .setConnectTimeout( CONNECTION_TIMEOUT )
                .setSocketTimeout( SOCKET_TIMEOUT )
                .build();

        final HttpClientBuilder builder = HttpClients.custom().setConnectionManager( ccm ).setDefaultRequestConfig( rc )
                .setRetryHandler( ( exception, executionCount, context ) ->
                        {
                            if ( executionCount > 3 ) {
                                return false;
                            }
                            if ( exception instanceof NoHttpResponseException ) {
                                logger.info( "NoHttpResponse start to retry times:" + executionCount );
                                return true;
                            }
                            if ( exception instanceof SocketTimeoutException ) {
                                logger.info( "SocketTimeout start to retry times:" + executionCount );
                                return true;
                            }
                            if ( exception instanceof ConnectionPoolTimeoutException ) {
                                logger.info( "ConnectionPoolTimeout start to retry times:" + executionCount );
                                return true;
                            }
                            return false;
                        }
                );

        final String proxy = config.getProxy();
        proxyHost = proxy;
        if ( proxy != null )
        {
            final int portSep = proxy.lastIndexOf( ':' );

            if ( portSep > -1 )
            {
                proxyHost = proxy.substring( 0, portSep );
                proxyPort = Integer.parseInt( proxy.substring( portSep + 1 ) );
            }
            final HttpRoutePlanner planner = new DefaultProxyRoutePlanner( new HttpHost( proxyHost, proxyPort ) );

            builder.setRoutePlanner( planner );
        }

        client = builder.build();

        artifactListReaders = new ArrayList<>();
        artifactListReaders.add( new FoloReportArtifactListReader() );
        artifactListReaders.add( new PlaintextArtifactListReader() );
        artifactListReaders.add( new PomArtifactListReader( config.getMavenSettingsXml(), config.getMavenTypeMapping() ) );
    }

    /**
     * Calls {@link #download(ArtifactList, OfflinerRequest, Set, ExecutorCompletionService, DefaultBeeline)} for each input location,
     * which spawns a bunch of new {@link Callable} instances, each responsible for downloading a single file, then
     * returns the number of new Callables added. Then, this method retrieves the next completed download from the
     * {@link ExecutorCompletionService} that manages the download tasks, logging results and iterating until all
     * downloads are complete. Finally, this method orchestrates metadata generation based on the contents of the
     * target directory (including files that were there before the download began), stats reporting.
     *
     * @return OfflinerResult that contains the original request plus the downloaded, avoided, and error captures for all
     * artifacts included in the lists / list files from the request.
     */
    public OfflinerResult copyOffline( OfflinerRequest request, DefaultBeeline beeline, Span rootSpan )
            throws IOException, OfflinerException, ExecutionException, InterruptedException
    {
        logger.info( "Planning download from:\n  " + StringUtils.join( request.getRepositoryUrls(), "\n  " ) );

        List<ArtifactList> artifactLists = request.getArtifactLists();

        final List<String> files = request.getArtifactListFiles();
        for ( final String filepath : files )
        {
            File file = new File( filepath );
            ArtifactListReader reader = getArtifactListReader( file );
            ArtifactList artifactList = reader.readPaths( file );
            if ( artifactList != null )
            {
                artifactLists.add( artifactList );
            }

            logger.info( "Downloading up to " + artifactList.size() + " artifacts from: " + filepath );
        }

        if ( artifactLists.isEmpty() )
        {
            logger.warn( "Nothing to do!" );
            return OfflinerResult.noAction( request );
        }

        ExecutorCompletionService<DownloadResult> executor =
                new ExecutorCompletionService<>( executorService );

        OfflinerResult runResult = new OfflinerResult( request );
        try
        {
            long start = System.nanoTime();
            Set<String> seen = new HashSet<>();
            int total = 0;
            for ( final ArtifactList artifactList : artifactLists )
            {
                logger.info( "Downloading up to {} artifacts from: {}", artifactList.size(), artifactList );
                total += download( artifactList, request, seen, executor, beeline );
            }
            for ( int i = 0; i < total; i++ )
            {
                logger.info( "Waiting for {} downloads\n", ( total - i ) );

                Future<DownloadResult> task = executor.take();
                DownloadResult result = task.get();
                if ( result == null )
                {
                    logger.error( "BUG: DownloadResult returned from execution should NEVER be null!" );
                }
                else if ( result.isSuccess() )
                {
                    runResult.addDownloaded();
                    logger.debug( "<<<SUCCESS: {}\n", result.getPath() );
                }
                else if ( result.isAvoided() )
                {
                    runResult.addAvoided();
                    logger.debug( "<<<Avoided: {}\n", result.getPath() );
                }
                else if ( result.getWarn() != null )
                {
                    runResult.addWarn( result.getPath(), result.getWarn() );
                    logger.debug( "<<<WARN: {}\n", result.getPath() );
                }
                else
                {
                    runResult.addError( result.getPath(), result.getError() );
                    logger.debug( "<<<FAIL: {}\n", result.getPath() );
                }
            }
            if ( rootSpan != null )
            {
                long end = System.nanoTime();
                double timing = ( end-start ) / NANOS_PER_MILLISECOND;
                rootSpan.addField( "download_timing_ms", timing );
                rootSpan.addField( "download_total", total );
                rootSpan.addField( "download_throughput", total / ( timing / 1000 ) );
            }
            Set<String> pomPaths = new HashSet<>();
            File download = request.getDownloadDirectory().getAbsoluteFile();

            searchForPomPaths( download, download.getPath(), pomPaths );

            if ( !request.isMetadataSkipped() )
            {
                long startMeta = System.nanoTime();
                generateMetadata( pomPaths, download.getPath() );
                if ( rootSpan != null )
                {
                    long endMeta = System.nanoTime();
                    rootSpan.addField( "generate_metadata_ms", ( endMeta - startMeta ) / NANOS_PER_MILLISECOND );
                }
            }


        }
        finally
        {
            IOUtils.closeQuietly( client );
        }

        return runResult;
    }

    /**
     * Parse the given manifest file path (with the appropriate {@link ArtifactListReader}, depending on the file
     * type. Use the resulting {@link ArtifactList} to generate a series of new download tasks, subtracting any
     * URLs that have been added from artifact lists that have already been processed.
     * @param artifactList The artifact list to download
     * @param seen The list of URLs that are already slated for download (or were pre-existing in the target directory)
     * @param executor
     * @return The number of new download tasks added from this artifact listing
     * @throws IOException In case the artifact list file cannot be read
     * @throws OfflinerException In case the artifact list file is not in a valid format (won't parse)
     */
    private int download( final ArtifactList artifactList, final OfflinerRequest request, Set<String> seen,
                          final ExecutorCompletionService<DownloadResult> executor, final DefaultBeeline beeline )
    {
        final List<String> paths;
        List<String> baseUrls = request.getRepositoryUrls();
        Map<String, String> checksums;

        if ( baseUrls == null || baseUrls.isEmpty() || OfflinerRequest.DEFAULT_URLS.equals( baseUrls ) )
        {
            baseUrls = artifactList.getRepositoryUrls();
            if ( baseUrls == null || baseUrls.isEmpty() )
            {
                baseUrls = OfflinerRequest.DEFAULT_URLS;
            }
        }

        paths = artifactList.getPaths();
        checksums = artifactList.getChecksums();
        if ( checksums == null )
        {
            checksums = new HashMap<>();
        }

        if ( paths == null || paths.isEmpty() )
        {
            logger.warn( "Nothing to download!" );
            return 0;
        }

        patchPathsForDownload( paths );

        int count = 0;
        BasicCookieStore cookieStore = new BasicCookieStore();

        for ( final String path : paths )
        {
            if ( !seen.contains( path ) )
            {
                executor.submit( newDownloader( request, path, checksums, baseUrls, cookieStore, beeline ) );
                count++;
            }
        }
        return count;
    }

    /**
     * Select the most appropriate {@link ArtifactListReader} for the given file. This will be used to parse the list
     * of files to download, along with any checksum metadata that might be available (depending on the format).
     * @param file The artifact-list file
     * @return The {@link ArtifactListReader} that should be used to parse the file
     * @throws OfflinerException In case there is no reader to handle the given file type
     */
    private ArtifactListReader getArtifactListReader( File file )
            throws OfflinerException
    {
        for ( ArtifactListReader reader : artifactListReaders )
        {
            if ( reader.supports( file ) )
            {
                return reader;
            }
        }
        throw new OfflinerException( "No reader supports file %s.", file.getPath() );
    }

    /**
     * Creates a new {@link Callable} capable of downloading a single file from a path and a set of base URLs, or
     * determining that the file has already been downloaded. If the checksums map is given, attempt to verify the
     * checksum of the file in the target directory or the stream as it's being downloaded.
     * @param offlinerRequest The offliner request, including list of base URLs representing the repositories from which files
     *                should be downloaded. Each base URL will be tried in order when downloading a path, until one works.
     * @param path The path to attempt to download from one of the repositories given in baseUrls
     * @param checksums The map of path -> checksum to use when attempting to verify the integrity of existing files or
     *                  the download stream
     * @param baseUrls
     * @param cookieStore
     * @return The Callable that will perform the actual download. At this point it will NOT have been queued for
     * execution.
     */
    private Callable<DownloadResult> newDownloader( final OfflinerRequest offlinerRequest, final String path,
                                                    final Map<String, String> checksums, final List<String> baseUrls,
                                                    final CookieStore cookieStore, final DefaultBeeline beeline)
    {
        return () -> {
            Span downloadLatencySpan = beeline == null ? null : beeline.startSpan( "download latency" );
            long start = System.nanoTime();
            final String name = Thread.currentThread().getName();
            Thread.currentThread().setName( "download--" + path );
            try
            {
                final File target = new File( offlinerRequest.getDownloadDirectory(), path );

                if ( target.exists() )
                {
                    if ( null == checksums || checksums.isEmpty() || !checksums.containsKey( path ) || null == checksums
                            .get( path ) )
                    {
                        markLatency( start, downloadLatencySpan, "download_latency_nano" );
                        return DownloadResult.avoid( path, true );
                    }

                    byte[] b = FileUtils.readFileToByteArray( target );
                    String original = checksums.get( path );
                    String current = sha256Hex( b );

                    if ( original.equals( current ) )
                    {
                        markLatency( start, downloadLatencySpan, "download_latency_nano" );
                        return DownloadResult.avoid( path, true );
                    }
                }

                final File dir = target.getParentFile();
                dir.mkdirs();

                final File part = new File( dir, target.getName() + ".part" );
                part.deleteOnExit();

                int reposRemaining = baseUrls.size();
                for ( String baseUrl : baseUrls )
                {
                    reposRemaining--;
                    String url;
                    try
                    {
                        url = UrlUtils.buildUrl( baseUrl, path );
                    }
                    catch ( final Exception e )
                    {
                        markLatency( start, downloadLatencySpan, "download_latency_nano" );
                        return DownloadResult.error( path, e );
                    }

                    logger.info( ">>>Downloading: " + url );

                    final HttpClientContext context = new HttpClientContext();
                    context.setCookieStore( cookieStore );

                    final HttpGet request = new HttpGet( url );
                    try (CloseableHttpResponse response = client.execute( request, context ))
                    {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if ( statusCode == 200 )
                        {
                            try (ChecksumOutputStream out = new ChecksumOutputStream( new FileOutputStream( part )))
                            {
                                long startChecksum = System.nanoTime();
                                IOUtils.copy( response.getEntity().getContent(), out );
                                markLatency( startChecksum, downloadLatencySpan, "checksum_latency_nano" );
                                if ( checksums != null )
                                {
                                    String checksum = checksums.get( path );
                                    if ( checksum != null && !isBlank( checksum ) && !out.getChecksum().isMatch( checksum ) )
                                    {
                                        markLatency( start, downloadLatencySpan, "download_latency_nano" );
                                        return DownloadResult.error( path, new IOException(
                                                "Checksum mismatch on file: " + path + " (calculated: '" + out.getChecksum() + "'; expected: '" + checksum + "')" ) );
                                    }
                                }
                            }
                            part.renameTo( target );
                            markLatency( start, downloadLatencySpan, "download_latency_nano" );
                            return DownloadResult.success( baseUrl, path );
                        }
                        else if ( statusCode == 404 )
                        {
                            if ( path.endsWith( Offliner.MD5_SUFFIX ) || path.endsWith( Offliner.SHA_SUFFIX ) )
                            {
                                logger.warn( "<<<Not Found: " + url );
                                if ( reposRemaining == 0 )
                                {
                                    markLatency( start, downloadLatencySpan, "download_latency_nano" );
                                    return DownloadResult.warn( path, "WARN: downloading path " + path + " was not "
                                                    + "found in any of the provided repositories." );
                                }
                            }
                            logger.error( "<<<Not Found: " + url );
                            if ( reposRemaining == 0 )
                            {
                                markLatency( start, downloadLatencySpan, "download_latency_nano" );
                                return DownloadResult.error( path, new IOException(
                                        "Error downloading path: " + path + ". The artifact was not "
                                                + "found in any of the provided repositories." ) );
                            }
                        }
                        else
                        {
                            final String serverError = IOUtils.toString( response.getEntity().getContent() );

                            String message = String.format(
                                    "Error downloading path: %s.\n%s\nServer status: %s\nServer response was:\n%s\n%s",
                                    path, SEPARATING_LINE, response.getStatusLine(), serverError, SEPARATING_LINE );

                            if ( reposRemaining == 0 )
                            {
                                markLatency( start, downloadLatencySpan, "download_latency_nano" );
                                return DownloadResult.error( path, new IOException( message ) );
                            }
                            else
                            {
                                logger.error( "<<<" + message );
                            }
                        }

                    }
                    catch ( final IOException e )
                    {
                        if ( logger.isTraceEnabled() )
                        {
                            logger.error( "Download failed for: " + url, e );
                        }
                        markLatency( start, downloadLatencySpan, "download_latency_nano" );
                        return DownloadResult.error( path, new IOException( "URL: " + url + " failed.", e ) );
                    }
                    finally
                    {
                        request.releaseConnection();
                        request.reset();
                    }
                }
            }
            finally
            {
                Thread.currentThread().setName( name );
            }
            markLatency( start, downloadLatencySpan, "download_latency_nano" );
            return null;
        };
    }

    @Override
    public void close()
            throws Exception
    {
        if ( executorService != null )
        {
            executorService.shutdown();
            executorService.awaitTermination( 30, TimeUnit.SECONDS );
        }
    }
}
