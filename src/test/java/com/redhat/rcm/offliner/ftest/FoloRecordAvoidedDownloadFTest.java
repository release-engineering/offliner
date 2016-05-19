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
package com.redhat.rcm.offliner.ftest;

import com.redhat.rcm.offliner.Main;
import com.redhat.rcm.offliner.Options;
import com.redhat.rcm.offliner.folo.StoreKey;
import com.redhat.rcm.offliner.folo.StoreType;
import com.redhat.rcm.offliner.folo.TrackedContentDTO;
import com.redhat.rcm.offliner.folo.TrackedContentEntryDTO;
import com.redhat.rcm.offliner.folo.TrackingKey;
import com.redhat.rcm.offliner.ftest.fixture.TestRepositoryServer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test the avoided result of re-download existing files.
 */
public class FoloRecordAvoidedDownloadFTest
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

        Main firstMain = run( opts );
        assertThat( "Wrong number of downloads logged. Should have been 1.", firstMain.getDownloaded(),
                    equalTo( 1 ) );

        //re-run to test the function of avoiding re-downloading existing files
        Main secondMain = run( opts );
        assertThat( "Wrong number of downloads logged. Should have been 0.", secondMain.getDownloaded(),
                    equalTo( 0 ) );
        assertThat( "Wrong number of avoided downloads logged. Should have been 1", secondMain.getAvoided(),
                    equalTo( 1 ) );
        assertThat( "Errors should be empty!", secondMain.getErrors().isEmpty(), equalTo( true ) );
    }
}
