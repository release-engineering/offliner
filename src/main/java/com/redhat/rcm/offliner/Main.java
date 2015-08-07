/**
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
package com.redhat.rcm.offliner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.AbstractExecutionAwareRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.kohsuke.args4j.CmdLineException;

public class Main
{
    private static final String SEP = "---------------------------------------------------------------";

    public static void main( final String[] args )
    {
        final Options opts = new Options();
        boolean start = false;
        try
        {
            start = opts.parseArgs( args );
        }
        catch ( final CmdLineException e )
        {
            e.printStackTrace();
            System.err.println( "Cannot start. See error output above." );
            System.exit( 1 );
        }

        if ( start )
        {
            try
            {
                new Main( opts ).run();
            }
            catch ( final MalformedURLException e )
            {
                e.printStackTrace();
                System.err.println( "Cannot initialize HTTP client / context. See error output above." );
                System.exit( 2 );
            }
        }
    }

    private final Options opts;

    private CloseableHttpClient client;

    private HttpClientContext contextPrototype;

    private ExecutorService executor;

    private volatile int counter = 0;

    private ConcurrentHashMap<String, Throwable> errors;

    private List<ArtifactListReader> artifactListReaders;


    public Main( final Options opts )
        throws MalformedURLException
    {
        this.opts = opts;
        init();
    }


    private void run()
    {
        final List<String> files = opts.getLocations();
        if ( files == null || files.isEmpty() )
        {
            System.out.println( "Nothing to do!" );
            return;
        }

        try
        {
            for ( final String file : files )
            {
                download( file );
            }

            logErrors();

            executor.shutdown();
            executor.awaitTermination( 30, TimeUnit.SECONDS );
        }
        catch ( final InterruptedException e )
        {
            System.err.println( "Interrupted waiting for download executor to shutdown." );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    private void logErrors()
    {
        if ( !errors.isEmpty() )
        {
            System.err.println( "There were download errors. See " + Options.ERROR_LOG
                                + " for details." );
        }

        final File errorLog = new File( Options.ERROR_LOG );
        try (PrintWriter writer = new PrintWriter( new FileWriter( errorLog ) ))
        {
            for ( final Map.Entry<String, Throwable> entry : errors.entrySet() )
            {
                writer.printf( "Path: %s\n%s\n", entry.getKey(), SEP );
                entry.getValue()
                     .printStackTrace( writer );
                writer.printf( "\n%s\n\n", SEP );
            }
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
            System.err.println( "Failed to write download errors to: " + Options.ERROR_LOG
                                + ". See above for more information." );
        }
    }

    private void download( final String filepath )
    {
        System.out.println( "Downloading artifacts from: " + filepath );
        try
        {
            File file = new File( filepath );
            ArtifactListReader reader = getArtifactListReader( file );
            ArtifactList artifactList = reader.readPaths( file );
            final List<String> paths = artifactList.getPaths();
            final List<String> baseUrls;
            if ( opts.getBaseUrl() == null )
            {
                baseUrls = artifactList.getRepositoryUrls();
                if ( baseUrls.isEmpty() )
                {
                    baseUrls.add( Options.DEFAULT_REPO_URL );
                }
            }
            else
            {
                baseUrls = Collections.singletonList( opts.getBaseUrl() );
            }
            for ( final String path : paths )
            {
                executor.execute( newDownloader( baseUrls, path ) );
            }
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
            System.err.printf( "\n\nFailed to read paths from file: %s. See above for more information.\n", filepath );
        }

        while ( counter > 0 )
        {
            System.out.println( "Waiting for " + counter + " downloads to complete." );
            synchronized ( this )
            {
                try
                {
                    wait( 1000 );
                }
                catch ( final InterruptedException e )
                {
                    System.err.println( "Interrupted waiting for downloads to complete for: " + filepath );
                    break;
                }
            }
        }
    }

    private ArtifactListReader getArtifactListReader( File file )
    {
        for ( ArtifactListReader reader : artifactListReaders )
        {
            if ( reader.supports( file ) )
            {
                return reader;
            }
        }
        throw new RuntimeException( "No reader supports file " + file.getPath() + '.' );
    }

    private Runnable newDownloader( final List<String> baseUrls, final String path )
    {
        return ( ) -> {
            final String name = Thread.currentThread()
                                      .getName();
            Thread.currentThread()
                  .setName( "download--" + path );
            try
            {
                counter++;

                final File target = new File( opts.getDownloads(), path );
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
                        errors.put( path, e );
                        return;
                    }

                    System.out.println( ">>>Downloading: " + url );

                    final HttpClientContext context = new HttpClientContext( contextPrototype );
                    final HttpGet request = new HttpGet( url );
                    try (CloseableHttpResponse response = client.execute( request, context ))
                    {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if ( statusCode == 200 )
                        {
                            try (FileOutputStream out = new FileOutputStream( part ))
                            {
                                IOUtils.copy( response.getEntity()
                                              .getContent(), out );

                                out.close();
                            }
                            part.renameTo( target );

                            System.out.println( "<<<Downloaded: " + url );

                            break;
                        }
                        else if ( statusCode == 404 )
                        {
                            System.out.println( "<<<Not Found: " + url );
                            if ( reposRemaining == 0 )
                            {
                                throw new IOException( "Error downloading path: " + path + ". The artifact was not "
                                                + "found in any of the provided repositories." );
                            }
                        }
                        else
                        {
                            final String serverError = IOUtils.toString( response.getEntity()
                                                                         .getContent() );
                            throw new IOException( "Error downloading path: " + path + ".\n" + SEP + "\nServer status: "
                                            + response.getStatusLine() + "\nServer response was:\n" + serverError + "\n"
                                            + SEP + "\n\nLocal stack trace:" );
                        }

                    }
                    catch ( final IOException e )
                    {
                        System.out.println( "FAIL: " + url );
                        errors.put( path, e );
                    }
                    finally
                    {
                        if ( request != null )
                        {
                            request.releaseConnection();

                            if ( request instanceof AbstractExecutionAwareRequest )
                            {
                                ( (AbstractExecutionAwareRequest) request ).reset();
                            }
                        }
                    }
                }
            }
            finally
            {
                counter--;
                synchronized ( Main.this )
                {
                    Main.this.notifyAll();
                }

                Thread.currentThread()
                      .setName( name );
            }
        };
    }

    private void init()
        throws MalformedURLException
    {
        executor = Executors.newCachedThreadPool( ( final Runnable r ) -> {
            final Thread t = new Thread( r );
            t.setDaemon( true );

            return t;
        } );

        errors = new ConcurrentHashMap<String, Throwable>();

        final PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager();
        ccm.setMaxTotal( opts.getConnections() );

        final HttpClientBuilder builder = HttpClients.custom()
                                                     .setConnectionManager( ccm );

        final String proxy = opts.getProxy();
        String proxyHost = proxy;
        int proxyPort = 8080;
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

        final CredentialsProvider creds = new BasicCredentialsProvider();

        contextPrototype = HttpClientContext.create();
        contextPrototype.setCredentialsProvider( creds );

        final String url = opts.getBaseUrl();
        if ( url != null )
        {
            final URL u = new URL( url );
            final AuthScope as = new AuthScope( u.getHost(), UrlUtils.getPort( u ) );

            final String user = opts.getUser();
            if ( user != null )
            {
                creds.setCredentials( as, new UsernamePasswordCredentials( user, opts.getPassword() ) );
            }
        }

        if ( proxy != null )
        {
            final String proxyUser = opts.getProxyUser();
            if ( proxyUser != null )
            {
                creds.setCredentials( new AuthScope( proxyHost, proxyPort ),
                                      new UsernamePasswordCredentials( proxyUser, opts.getProxyPassword() ) );
            }

        }

        String baseUrl = opts.getBaseUrl();
        if ( baseUrl == null )
        {
            baseUrl = Options.DEFAULT_REPO_URL;
        }

        artifactListReaders = new ArrayList<>( 2 );
        artifactListReaders.add( new PlaintextArtifactListReader( baseUrl ) );
        artifactListReaders.add( new PomArtifactListReader( opts.getSettingsXml(), opts.getTypeMapping(), creds ) );
    }

}
