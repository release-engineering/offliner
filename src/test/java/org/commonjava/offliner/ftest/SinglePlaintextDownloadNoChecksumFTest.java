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
package org.commonjava.offliner.ftest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.offliner.Offliner;
import org.commonjava.offliner.OfflinerResult;
import org.commonjava.offliner.cli.Options;
import org.commonjava.offliner.ftest.fixture.TestRepositoryServer;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Create a one-path plaintext artifact list + content, then use an overridden repoUrl list (to the test server fixture) to
 * download the content. The artifact path entry will NOT contain a checksum, or even a comma delimiter. This verifies
 * compatibility with the simplest input file type.
 *
 * Created by jdcasey on 4/20/16.
 */
public class SinglePlaintextDownloadNoChecksumFTest
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
        TestRepositoryServer server = newRepositoryServer();

        // Generate some test content
        String path = contentGenerator.newArtifactPath( "jar" );
        byte[] content = contentGenerator.newBinaryContent( 1024 );

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.registerContent( path, content );
        server.registerContent( path + Offliner.SHA_SUFFIX, sha1Hex( content ) );
        server.registerContent( path + Offliner.MD5_SUFFIX, md5Hex( content ) );

        // Write the plaintext file we'll use as input.
        File plaintextList = temporaryFolder.newFile( "artifact-list." + getClass().getSimpleName() + ".txt" );
        String pathWithChecksum = contentGenerator.newPlaintextEntryWithoutChecksum( path );
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
        System.out.printf( "ERRORS:\n\n%s\n\n\n", StringUtils.join( errors.keySet()
                                                                          .stream()
                                                                          .map( k -> "ERROR: " + k + ": " + errors.get( k ).getMessage()
                                                                                  + "\n  " + StringUtils.join(
                                                                                  errors.get( k ).getStackTrace(), "\n  " ) ).collect(
                        Collectors.toList() ), "\n\n") );

        assertThat( "Wrong number of downloads logged. Should have been 3 including checksums.", finishedMain.getDownloaded(),
                    equalTo( 3 ) );
        assertThat( "Errors should be empty!", finishedMain.getErrors().isEmpty(), equalTo( true ) );

        File downloaded = new File( downloads, path );
        assertThat( "File: " + path + " doesn't seem to have been downloaded!", downloaded.exists(), equalTo( true ) );
        assertThat( "Downloaded file: " + path + " contains the wrong content!",
                    FileUtils.readFileToByteArray( downloaded ), equalTo( content ) );
    }
}
