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

import com.redhat.red.offliner.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import com.redhat.red.offliner.Offliner;
import com.redhat.red.offliner.OfflinerResult;
import com.redhat.red.offliner.ftest.fixture.TestRepositoryServer;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test SKIPPING the generating of maven-metadata.xml from files downloaded successfully or avoided
 */
public class SkipMavenMetadataGenerateFTest
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
        server.registerContent( path + Offliner.SHA_SUFFIX, sha1Hex( content ) );
        server.registerContent( path + Offliner.MD5_SUFFIX, md5Hex( content ) );

        // All deps imply an accompanying POM file when using the POM artifact list reader, so we have to register one of these too.
        Model pomDep = contentGenerator.newPomFor( dep );
        String pomPath = contentGenerator.pathOf( pomDep );
        String md5Path = pomPath + Offliner.MD5_SUFFIX;
        String shaPath = pomPath + Offliner.SHA_SUFFIX;

        String pomStr = contentGenerator.pomToString( pomDep );

        server.registerContent( pomPath, pomStr );
        server.registerContent( md5Path, md5Hex( pomStr ) );
        server.registerContent( shaPath, sha1Hex( pomStr ) );

        // Write the plaintext file we'll use as input.
        File pomFile = temporaryFolder.newFile( getClass().getSimpleName() + ".pom" );

        FileUtils.write( pomFile, contentGenerator.pomToString( pom ) );

        Options opts = new Options();
        opts.setBaseUrls( Collections.singletonList( server.getBaseUri() ) );

        // Capture the downloads here so we can verify the content.
        File downloads = temporaryFolder.newFolder();

        opts.setDownloads( downloads );
        opts.setLocations( Collections.singletonList( pomFile.getAbsolutePath() ) );

        // THIS IS THE KEY TO THIS TEST.
        opts.setSkipMetadata( true );

        // run `new Main(opts).run()` and return the Main instance so we can query it for errors, etc.
        OfflinerResult finishedMain = run( opts );
        assertThat( "Errors should be empty!", finishedMain.getErrors().isEmpty(), equalTo( true ) );

        // get the ProjectVersion info by pomPath reading from ArtifactPathInfo parse.
        ArtifactPathInfo artifactPathInfo = ArtifactPathInfo.parse( pomPath );
        ProjectVersionRef gav = artifactPathInfo.getProjectId();
        File metadataFile =
                Paths.get( opts.getDownloads().getAbsolutePath(), gav.getGroupId().replace( '.', File.separatorChar ),
                           gav.getArtifactId(), "maven-metadata.xml" ).toFile();
        assertThat(
                "maven-metadata.xml for path: " + metadataFile.getParent() + " doesn't seem to have been generated!",
                metadataFile.exists(), equalTo( false ) );
    }
}
