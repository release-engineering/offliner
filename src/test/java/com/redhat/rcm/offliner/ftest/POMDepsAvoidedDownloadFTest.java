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
import com.redhat.rcm.offliner.ftest.fixture.TestRepositoryServer;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test the avoided result of re-download existing files.
 */
public class POMDepsAvoidedDownloadFTest
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

        Dependency dep = contentGenerator.newDependency();
        Model pom = contentGenerator.newPom();
        pom.addDependency( dep );

        String path = contentGenerator.pathOf( dep );

        // Register the generated content by writing it to the path within the repo server's dir structure.
        // This way when the path is requested it can be downloaded instead of returning a 404.
        server.registerContent( path, content );

        // All deps imply an accompanying POM file when using the POM artifact list reader, so we have to register one of these too.
        Model pomDep = contentGenerator.newPomFor( dep );
        String pomPath = contentGenerator.pathOf( pomDep );

        server.registerContent( pomPath, contentGenerator.pomToString( pomDep ) );

        // Write the plaintext file we'll use as input.
        File pomFile = temporaryFolder.newFile( getClass().getSimpleName() + ".pom" );

        FileUtils.write( pomFile, contentGenerator.pomToString( pom ) );

        Options opts = new Options();
        opts.setBaseUrls( Collections.singletonList( server.getBaseUri() ) );

        // Capture the downloads here so we can verify the content.
        File downloads = temporaryFolder.newFolder();

        opts.setDownloads( downloads );
        opts.setLocations( Collections.singletonList( pomFile.getAbsolutePath() ) );

        Main firstMain = run( opts );
        assertThat( "Wrong number of downloads logged. Should have been 2 (declared jar + its corresponding POM).",
                    firstMain.getDownloaded(), equalTo( 2 ) );

        //re-run to test the function of avoiding re-downloading existing files
        Main secondMain = run( opts );
        assertThat( "Wrong number of downloads logged. Should have been 0.", secondMain.getDownloaded(),
                    equalTo( 0 ) );
        assertThat( "Wrong number of avoided downloads logged. Should have been 2", secondMain.getAvoided(),
                    equalTo( 2 ) );
        assertThat( "Errors should be empty!", secondMain.getErrors().isEmpty(), equalTo( true ) );
    }
}
