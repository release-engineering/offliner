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
package com.redhat.red.offliner.ftest;

import com.redhat.red.offliner.Main;
import com.redhat.red.offliner.Options;
import com.redhat.red.offliner.folo.StoreKey;
import com.redhat.red.offliner.folo.StoreType;
import com.redhat.red.offliner.folo.TrackedContentDTO;
import com.redhat.red.offliner.folo.TrackedContentEntryDTO;
import com.redhat.red.offliner.folo.TrackingKey;
import com.redhat.red.offliner.ftest.fixture.TestRepositoryServer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Create a one-path content record as if it's from Indy's Folo add-on, then use an overridden repoUrl list (to the test
 * server fixture) to download the content. The record will contain an origin URL but NOT a local URL, so we check the
 * ability to download from the server upstream of Indy if that URL is present.
 *
 * Created by jdcasey on 4/20/16.
 */
public class SingleFoloRecordDownloadFTest
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
        byte[] content = contentGenerator.newBinaryContent( 1024 );

        TrackedContentEntryDTO dto =
                contentGenerator.newRemoteContentEntry( new StoreKey( StoreType.remote, "test" ), "jar",
                                                        server.getBaseUri(), content );

        TrackedContentDTO record = new TrackedContentDTO( new TrackingKey( "test-record" ), Collections.emptySet(),
                                                          Collections.singleton( dto ) );

        String path = dto.getPath();

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.registerContent( path, content );

        // Write the plaintext file we'll use as input.
        File foloRecord = temporaryFolder.newFile( "folo." + getClass().getSimpleName() + ".json" );

        FileUtils.write( foloRecord, objectMapper.writeValueAsString( record ) );

        Options opts = new Options();
        opts.setBaseUrls( Collections.singletonList( server.getBaseUri() ) );

        // Capture the downloads here so we can verify the content.
        File downloads = temporaryFolder.newFolder();

        opts.setDownloads( downloads );
        opts.setLocations( Collections.singletonList( foloRecord.getAbsolutePath() ) );

        // run `new Main(opts).run()` and return the Main instance so we can query it for errors, etc.
        Main finishedMain = run( opts );

        assertThat( "Wrong number of downloads logged. Should have been 1.", finishedMain.getDownloaded(),
                    equalTo( 1 ) );
        assertThat( "Errors should be empty!", finishedMain.getErrors().isEmpty(), equalTo( true ) );

        File downloaded = new File( downloads, path );
        assertThat( "File: " + path + " doesn't seem to have been downloaded!", downloaded.exists(), equalTo( true ) );
        assertThat( "Downloaded file: " + path + " contains the wrong content!",
                    FileUtils.readFileToByteArray( downloaded ), equalTo( content ) );
    }
}
