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

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test that an incorrect checksum on the downloaded file results in an error.
 */
public class ChecksumMismatchFTest
        extends AbstractOfflinerFunctionalTest
{
    @Test
    public void run()
            throws Exception
    {
        // We only need one repo server.
        TestRepositoryServer server = newRepositoryServer();

        // Generate some test content
        byte[] expectedContent = contentGenerator.newBinaryContent( 1024 );
        byte[] downloadedContent = contentGenerator.newBinaryContent( 1024 );

        TrackedContentEntryDTO dto =
                contentGenerator.newRemoteContentEntry( new StoreKey( StoreType.remote, "test" ), "jar",
                                                        server.getBaseUri(), expectedContent );

        TrackedContentDTO record = new TrackedContentDTO( new TrackingKey( "test-record" ), Collections.emptySet(),
                                                          Collections.singleton( dto ) );

        String path = dto.getPath();

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.registerContent( path, downloadedContent );
        server.registerContent( path + Main.SHA_SUFFIX, sha1Hex( downloadedContent ) );
        server.registerContent( path + Main.MD5_SUFFIX, md5Hex( downloadedContent ) );

        // Write the plaintext file we'll use as input.
        File foloRecord = temporaryFolder.newFile( "folo." + getClass().getSimpleName() + ".json" );

        FileUtils.write( foloRecord, objectMapper.writeValueAsString( record ) );

        Options opts = new Options();
        opts.setBaseUrls( Collections.singletonList( server.getBaseUri() ) );

        // Capture the downloads here so we can verify the content.
        File downloads = temporaryFolder.newFolder();

        opts.setDownloads( downloads );
        opts.setLocations( Collections.singletonList( foloRecord.getAbsolutePath() ) );

        Main main = run( opts );
        assertThat( "Wrong number of downloads logged. Should have been just the checksum files.", main.getDownloaded(), equalTo( 2 ) );
        assertThat( "Checksum mismatch should have resulted in an error", main.getErrors().isEmpty(),
                    equalTo( false ) );

        assertThat( "The downloaded file should not have been kept.", new File( downloads, path ).exists(), equalTo( false ) );
    }
}
