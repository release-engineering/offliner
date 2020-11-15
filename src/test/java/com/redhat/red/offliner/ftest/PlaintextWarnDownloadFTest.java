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

import com.redhat.red.offliner.Offliner;
import com.redhat.red.offliner.OfflinerResult;
import com.redhat.red.offliner.cli.Options;
import com.redhat.red.offliner.ftest.fixture.TestRepositoryServer;
import org.apache.commons.io.FileUtils;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Test;
import sun.security.util.ArrayUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test the warn result of md5, sha hash files downloading.
 */
public class PlaintextWarnDownloadFTest
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
        ExpectationServer server = new ExpectationServer();
        server.start();

        // Generate some test content
        String path = contentGenerator.newArtifactPath( "jar" );
        byte[] content = contentGenerator.newBinaryContent( 1024 );

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        // Register sha, md5 files for 404 response to simulate the missing hash files case.
        server.expect( "GET", server.formatUrl( path ), 200,  new ByteArrayInputStream( content ) );
        server.expect( "GET", server.formatUrl( path + Offliner.SHA_SUFFIX ), 404, sha1Hex( content ) );
        server.expect( "GET", server.formatUrl( path + Offliner.MD5_SUFFIX ), 404, md5Hex( content ) );

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

        OfflinerResult result = run( opts );
        assertThat( "Wrong number of downloads logged. Should have been 1.", result.getDownloaded(), equalTo( 1 ) );
        assertThat( "Wrong number of warn logged. Should have been 2", result.getWarns().size(),
                    equalTo( 2 ) );
        assertThat( "Errors should be empty!", result.getErrors().isEmpty(), equalTo(true) );
    }
}
