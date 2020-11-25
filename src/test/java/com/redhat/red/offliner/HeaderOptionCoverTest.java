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

import com.redhat.red.offliner.cli.Options;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static com.redhat.red.offliner.OfflinerUtils.processArgsWithHeader;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit Test for the header option supplying before download main is running.
 */
public class HeaderOptionCoverTest {

    @Test
    public void run() throws Exception {

        File header = new File( "header" );
        String headerContent = "no-metadata\n" +
                "download=repository-1\n" +
                "repo-url=repo.maven.apache.org\n" +
                "FILES=manifest-1.txt\n";
        writeByteArrayToFile( header, ( headerContent ).getBytes() );

        Options options = new Options();
        options.setHeaderFile( header );
        options.setDownloads( new File( "repository-2" ) );
        options.setBaseUrls( Collections.singletonList( "test.repository.org" ) );
        options.setLocations( Collections.singletonList( "manifest-2.txt" ) );

        assertThat( options.isSkipMetadata(), equalTo( false ) );
        assertThat( options.getDownloads(), equalTo( FileUtils.getFile( "repository-2" ) ) );
        assertThat( options.getBaseUrls(), equalTo( Collections.singletonList( "test.repository.org" ) ) );
        assertThat( options.getLocations(), equalTo( Collections.singletonList( "manifest-2.txt" ) ) );

        Options newOpts = processArgsWithHeader( options );

        assertThat( newOpts.isSkipMetadata(), equalTo( true ) );
        assertThat( newOpts.getDownloads(), equalTo( FileUtils.getFile( "repository-1" ) ) );
        assertThat( newOpts.getBaseUrls(), equalTo( Collections.singletonList( "repo.maven.apache.org" ) ) );
        assertThat( newOpts.getLocations(), equalTo( Collections.singletonList( "manifest-1.txt" ) ) );
    }
}