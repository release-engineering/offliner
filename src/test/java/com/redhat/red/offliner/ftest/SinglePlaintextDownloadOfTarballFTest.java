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
package com.redhat.red.offliner.ftest;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import com.redhat.red.offliner.Offliner;
import com.redhat.red.offliner.OfflinerResult;
import com.redhat.red.offliner.cli.Options;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Create a one-path plaintext artifact list + content, then use an overridden repoUrl list (to the test server fixture) to
 * download the content. The artifact path entry will NOT contain a checksum, or even a comma delimiter. This verifies
 * compatibility with the simplest input file type.
 *
 * Created by jdcasey on 4/20/16.
 */
public class SinglePlaintextDownloadOfTarballFTest
        extends AbstractOfflinerFunctionalTest
{
    @Test
    public void testTarball()
            throws IOException
    {
        // Generate some test content
        String path = contentGenerator.newArtifactPath( "tar.gz" );
        Map<String, byte[]> entries = new HashMap<>();
        entries.put(contentGenerator.newArtifactPath( "jar" ), contentGenerator.newBinaryContent( 2400 ) );
        entries.put(contentGenerator.newArtifactPath( "jar" ), contentGenerator.newBinaryContent( 2400 ) );

        makeTarball( entries );
    }

    @Test
    public void testGenericTarballDownload()
            throws Exception
    {
        // Generate some test content
        String path = contentGenerator.newArtifactPath( "tar.gz" );
        Map<String, byte[]> entries = new HashMap<>();
        entries.put(contentGenerator.newArtifactPath( "jar" ), contentGenerator.newBinaryContent( 2400 ) );
        entries.put(contentGenerator.newArtifactPath( "jar" ), contentGenerator.newBinaryContent( 2400 ) );

        final File tgz = makeTarball( entries );

        System.out.println("tar content array has length: " + tgz.length());

        // We only need one repo server.
        ExpectationServer server = new ExpectationServer();
        server.start();

        String url = server.formatUrl( path );

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.expect( "GET", url, (req, resp)->{
            //            Content-Length: 47175
            //            Content-Type: application/x-gzip
            resp.setHeader( "Content-Encoding", "x-gzip" );
            resp.setHeader( "Content-Type", "application/x-gzip" );

            byte[] raw = FileUtils.readFileToByteArray( tgz );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GzipCompressorOutputStream gzout = new GzipCompressorOutputStream( baos );
            gzout.write( raw );
            gzout.finish();

            byte[] content = baos.toByteArray();

            resp.setHeader( "Content-Length", Long.toString( content.length ) );
            OutputStream respStream = resp.getOutputStream();
            respStream.write( content );
            respStream.flush();

            System.out.println("Wrote content with length: " + content.length);
        } );

        final PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager();
        ccm.setMaxTotal( 1 );

        final HttpClientBuilder builder = HttpClients.custom().setConnectionManager( ccm );
        CloseableHttpClient client = builder.build();

        HttpGet get = new HttpGet(url);
//        get.setHeader( "Accept-Encoding", "gzip,deflate" );

        Boolean result = client.execute( get, (response)->{
            Arrays.stream( response.getAllHeaders() ).forEach( ( h ) -> System.out.println( "Header:: " + h ) );

            Header contentEncoding = response.getEntity().getContentEncoding();
            if ( contentEncoding == null )
            {
                contentEncoding = response.getFirstHeader( "Content-Encoding" );
            }

            System.out.printf( "Got content encoding: %s\n", contentEncoding == null ? "None" : contentEncoding.getValue() );

            byte[] content = IOUtils.toByteArray( response.getEntity().getContent() );

            try(TarArchiveInputStream tarIn =
                        new TarArchiveInputStream( new GzipCompressorInputStream( new ByteArrayInputStream( content ) ) ))
            {
                TarArchiveEntry entry = null;
                while( (entry = tarIn.getNextTarEntry()) != null )
                {
                    System.out.printf( "Got tar entry: %s\n", entry.getName() );
                    byte[] entryData = new byte[(int) entry.getSize()];
                    int read = tarIn.read( entryData, 0, entryData.length );
                }
            }

            return false;
        } );
    }

    /**
     * In general, we should only have one test method per functional test. This allows for the best parallelism when we
     * execute the tests, especially if the setup takes some time.
     *
     * @throws Exception In case anything (anything at all) goes wrong!
     */
    @Test
    public void run()
            throws Exception
    {
        // Generate some test content
        String path = contentGenerator.newArtifactPath( "tar.gz" );
        Map<String, byte[]> entries = new HashMap<>();
        entries.put(contentGenerator.newArtifactPath( "jar" ), contentGenerator.newBinaryContent( 2400 ) );
        entries.put(contentGenerator.newArtifactPath( "jar" ), contentGenerator.newBinaryContent( 2400 ) );

        final File tgz = makeTarball( entries );

        System.out.println("tar content array has length: " + tgz.length());

        // We only need one repo server.
        ExpectationServer server = new ExpectationServer();
        server.start();

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.expect( "GET", server.formatUrl( path ), (req, resp)->{
//            Content-Length: 47175
//            Content-Type: application/x-gzip
            resp.setHeader( "Content-Encoding", "gzip" );
            resp.setHeader( "Content-Type", "application/x-gzip" );

            byte[] raw = FileUtils.readFileToByteArray( tgz );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GzipCompressorOutputStream gzout = new GzipCompressorOutputStream( baos );
            gzout.write( raw );
            gzout.finish();

            byte[] content = baos.toByteArray();

            resp.setHeader( "Content-Length", Long.toString( content.length ) );
            OutputStream respStream = resp.getOutputStream();
            respStream.write( content );
            respStream.flush();

            System.out.println("Wrote content with length: " + content.length);
        } );

        byte[] content = FileUtils.readFileToByteArray( tgz );

        server.expect( "GET", server.formatUrl( path + Offliner.SHA_SUFFIX ), 200, sha1Hex( content ) );
        server.expect( "GET", server.formatUrl( path + Offliner.MD5_SUFFIX ), 200, md5Hex( content ) );

        // Write the plaintext file we'll use as input.
        File plaintextList = temporaryFolder.newFile( "artifact-list." + getClass().getSimpleName() + ".txt" );
        String pathWithChecksum = contentGenerator.newPlaintextEntryWithChecksum( path, content );
        FileUtils.write( plaintextList, pathWithChecksum );

        Options opts = new Options();
        opts.setBaseUrls( Collections.singletonList( server.getBaseUri() ) );

        // Capture the downloads here so we can verify the content.
        File downloads = temporaryFolder.newFolder();

        opts.setDownloads( downloads );
        opts.setLocations( Collections.singletonList( plaintextList.getAbsolutePath() ) );
        opts.setConnections( 1 );

        // run `new Main(opts).run()` and return the Main instance so we can query it for errors, etc.
        OfflinerResult finishedMain = run( opts );
        Map<String, Throwable> errors = finishedMain.getErrors();
        System.out.printf( "ERRORS:\n\n%s\n\n\n", StringUtils.join(errors.keySet()
                                                        .stream()
                                                        .map( k -> "ERROR: " + k + ": " + errors.get( k ).getMessage()
                                                                + "\n  " + StringUtils.join(
                                                                errors.get( k ).getStackTrace(), "\n  " ) ).collect(
                        Collectors.toList() ), "\n\n") );

        File downloaded = new File( downloads, path );
        assertThat( "File: " + path + " doesn't seem to have been downloaded!", downloaded.exists(), equalTo( true ) );
//        assertThat( "Downloaded file: " + path + " contains the wrong content!",
//                    FileUtils.readFileToByteArray( downloaded ), equalTo( content ) );

        File tarball = new File( downloads, path );
        System.out.println( "Length of downloaded file: " + tarball.length() );

        File tmp = new File( "/tmp/download.tar.gz" );
        File tmp2 = new File( "/tmp/content.tar.gz" );
        FileUtils.writeByteArrayToFile( tmp2, content );
        FileUtils.copyFile( tarball, tmp );

        try(TarArchiveInputStream tarIn =
                new TarArchiveInputStream( new GzipCompressorInputStream( new FileInputStream( tarball ) ) ))
        {
            TarArchiveEntry entry = null;
            while( (entry = tarIn.getNextTarEntry()) != null )
            {
                byte[] entryData = new byte[(int) entry.getSize()];
                int read = tarIn.read( entryData, 0, entryData.length );
                assertThat( "Not enough bytes read for: " + entry.getName(), read, equalTo( (int) entry.getSize() ) );
                assertThat( entry.getName() + ": data doesn't match input",
                            Arrays.equals( entries.get( entry.getName() ), entryData ), equalTo( true ) );
            }
        }

        assertThat( "Wrong number of downloads logged. Should have been 3 including checksums.", finishedMain.getDownloaded(),
                    equalTo( 3 ) );
        assertThat( "Errors should be empty!", errors.isEmpty(), equalTo( true ) );

    }

    private File makeTarball( final Map<String, byte[]> entries )
            throws IOException
    {
        File tgz = temporaryFolder.newFile();
        try (TarArchiveOutputStream tarOut = new TarArchiveOutputStream( new GzipCompressorOutputStream( new FileOutputStream( tgz ) ) ))
        {
            entries.forEach( (name,content)->{
                try
                {
                    File entryFile = temporaryFolder.newFile();
                    FileUtils.writeByteArrayToFile( entryFile, content );

                    TarArchiveEntry entry = new TarArchiveEntry( entryFile, name );
                    //                entry.setSize( content.length );
                    //                entry.setMode( 0644 );
                    //                entry.setGroupId( 1000 );
                    //                entry.setUserId( 1000 );

                    tarOut.putArchiveEntry( entry );

                    System.out.printf( "Entry: %s mode: '0%s'\n", entry.getName(), Integer.toString(entry.getMode(), 8) );

                    tarOut.write(content);
                    tarOut.closeArchiveEntry();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                    fail( "Failed to write tarball" );
                }
            } );

            tarOut.flush();
        }

        try(TarArchiveInputStream tarIn =
                    new TarArchiveInputStream( new GzipCompressorInputStream( new FileInputStream( tgz ) ) ))
        {
            TarArchiveEntry entry = null;
            while( (entry = tarIn.getNextTarEntry()) != null )
            {
                byte[] entryData = new byte[(int) entry.getSize()];
                int read = tarIn.read( entryData, 0, entryData.length );
                assertThat( "Not enough bytes read for: " + entry.getName(), read, equalTo( (int) entry.getSize() ) );
                assertThat( entry.getName() + ": data doesn't match input",
                            Arrays.equals( entries.get( entry.getName() ), entryData ), equalTo( true ) );
            }
        }

        return tgz;
    }
}
