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
package com.redhat.red.offliner;

import com.redhat.red.offliner.cli.Options;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static com.redhat.red.offliner.OfflinerUtils.parseArgsWithHeader;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit Test for the header options declared in manifest.
 */
public class ManifestWithHeaderTest {

    @Test
    public void run() throws Exception {

        File manifest = new File( "manifest" );
        String headerContent = "#header\n" +
                "no-metadata\n" +
                "download=repository-1\n" +
                "repo-url=test.repo.A\n" +
                "----\n";
        writeByteArrayToFile( manifest, ( headerContent ).getBytes() );

        String[] argsA = { manifest.getAbsolutePath() };
        Options optionsA = new Options();
        optionsA.parseArgs( argsA );

        assertThat( argsA.length, equalTo( 1 ) );
        assertThat( optionsA.isSkipMetadata(), equalTo( false ) );
        assertThat( optionsA.getDownloads(), equalTo( FileUtils.getFile( "repository" ) ) );
        assertThat( optionsA.getBaseUrls(), equalTo( null ) );

        String[] argsB = parseArgsWithHeader( argsA );
        Options optionsB = new Options();
        optionsB.parseArgs( argsB );

        assertThat( argsB.length, equalTo( 6 ) );
        assertThat( optionsB.isSkipMetadata(), equalTo( true ) );
        assertThat( optionsB.getDownloads(), equalTo( FileUtils.getFile( "repository-1" ) ) );
        assertThat( optionsB.getBaseUrls(), equalTo( Collections.singletonList( "test.repo.A" ) ) );

        String[] argsC = { "--download", "repository-2", "--url", "test.repo.B", manifest.getAbsolutePath() };
        String[] newArgsC = parseArgsWithHeader( argsC );
        Options optionsC = new Options();
        optionsC.parseArgs( newArgsC );
        assertThat( newArgsC.length, equalTo( 10 ) );
        assertThat( optionsC.isSkipMetadata(), equalTo( true ) );
        assertThat( optionsC.getDownloads(), equalTo( FileUtils.getFile( "repository-2" ) ) );
        assertThat( optionsC.getBaseUrls().size(), equalTo( 2 ) );
    }
}