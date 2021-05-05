/*
 * Copyright (C) 2015 Red Hat, Inc.
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

import com.redhat.red.offliner.Offliner;
import com.redhat.red.offliner.OfflinerResult;
import com.redhat.red.offliner.cli.Options;
import org.apache.commons.io.FileUtils;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Create a one-path plaintext artifact list + content, then use an overridden repoUrl list (to the test server fixture) to
 * download the content. The test server will redirect this request back to itself on a different path, testing use of
 * HTTP redirection.
 *
 * Created by jdcasey on 4/20/16.
 */
public class DownloadWithRedirectFTest
        extends AbstractOfflinerFunctionalTest
{
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
        // We only need one repo server.
        ExpectationServer server = new ExpectationServer().start();
//        TestRepositoryServer server = newRepositoryServer();

        // Generate some test content
        String path = contentGenerator.newArtifactPath( "jar" );
        byte[] content = contentGenerator.newBinaryContent( 1024 );

        // register the redirects.
        server.expect( "GET", "/" + path, (request, response)->
        {
            response.setStatus( 302 );
            response.setHeader( "Location", server.formatUrl( "/redir/" + path ) );
        } );

        server.expect( "GET", "/" + path + Offliner.SHA_SUFFIX, ( request, response)->
        {
            response.setStatus( 302 );
            response.setHeader( "Location", server.formatUrl( "/redir/" + path + Offliner.SHA_SUFFIX ) );
        } );

        server.expect( "GET", "/" + path + Offliner.MD5_SUFFIX, (request, response)->
        {
            response.setStatus( 302 );
            response.setHeader( "Location", server.formatUrl( "/redir/" + path + Offliner.MD5_SUFFIX ) );
        } );

        server.expect( "HEAD", "/" + path, (request, response)->
        {
            response.setStatus( 302 );
            response.setHeader( "Location", server.formatUrl( "/redir/" + path ) );
        } );

        server.expect( "HEAD", "/" + path + Offliner.SHA_SUFFIX, (request, response)->
        {
            response.setStatus( 302 );
            response.setHeader( "Location", server.formatUrl( "/redir/" + path + Offliner.SHA_SUFFIX ) );
        } );

        server.expect( "HEAD", "/" + path + Offliner.MD5_SUFFIX, (request, response)->
        {
            response.setStatus( 302 );
            response.setHeader( "Location", server.formatUrl( "/redir/" + path + Offliner.MD5_SUFFIX ) );
        } );

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.expect( "/redir/" + path, 200, new ByteArrayInputStream( content ) );
        server.expect( "/redir/" + path + Offliner.SHA_SUFFIX, 200, sha1Hex( content ) );
        server.expect( "/redir/" + path + Offliner.MD5_SUFFIX, 200, md5Hex( content ) );

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

        // run `new Main(opts).run()` and return the Main instance so we can query it for errors, etc.
        OfflinerResult finishedMain = run( opts );

        assertThat( "Wrong number of downloads logged. Should have been 3 including checksums.", finishedMain.getDownloaded(),
                    equalTo( 3 ) );
        assertThat( "Errors should be empty!", finishedMain.getErrors().isEmpty(), equalTo( true ) );

        File downloaded = new File( downloads, path );
        assertThat( "File: " + path + " doesn't seem to have been downloaded!", downloaded.exists(), equalTo( true ) );
        assertThat( "Downloaded file: " + path + " contains the wrong content!",
                    FileUtils.readFileToByteArray( downloaded ), equalTo( content ) );
    }
}
