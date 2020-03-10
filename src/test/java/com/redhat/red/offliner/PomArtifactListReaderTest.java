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

import com.redhat.red.offliner.model.ArtifactList;
import com.redhat.red.offliner.alist.PomArtifactListReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PomArtifactListReaderTest
{

    private static final String TEMP_DIR = "target/temp";

    private static final String TEMP_POM_DIR = "target/temp/pom";

    @BeforeClass
    public static void prepare() throws IOException
    {
        File tempDir = new File( TEMP_POM_DIR );
        if ( tempDir.exists() )
        {
            FileUtils.deleteDirectory( tempDir );
        }
        tempDir.mkdirs();

        List<String> resources = new ArrayList<String>( 2 );
        resources.add( "repo.pom" );
        resources.add( "settings.xml" );

        for ( String resource : resources )
        {
            File target = new File( TEMP_POM_DIR, resource );
            try (InputStream is = PomArtifactListReaderTest.class.getClassLoader().getResourceAsStream( resource ); OutputStream os = new FileOutputStream( target ))
            {
                IOUtils.copy( is, os );
            }
        }
    }

    @AfterClass
    public static void cleanup() throws IOException
    {
        File tempDir = new File( TEMP_DIR );
        if ( tempDir.exists() )
        {
            FileUtils.deleteDirectory( tempDir );
        }
    }

    private File getFile( final String filename )
    {
        return new File( TEMP_POM_DIR, filename );
    }

    /**
     * Checks if dependencies are read correctly. It checks if all directly mentioned artifacts are present in the
     * result.
     */
    @Test
    public void readPathsDependencies() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> paths = artList.getPaths();
        checkPath( paths, "xml-resolver/xml-resolver/1.2/xml-resolver-1.2.jar" );
        checkPath( paths, "xml-apis/xml-apis/1.3.04/xml-apis-1.3.04.jar" );
        checkPath( paths, "org/apache/ant/ant/1.8.0/ant-1.8.0.pom" );
        checkPath( paths, "org/apache/ant/ant-launcher/1.8.0/ant-launcher-1.8.0.jar" );
        checkPath( paths, "org/apache/ant/ant-parent/1.8.0/ant-parent-1.8.0.pom" );
        checkPath( paths, "org/apache/apache/3/apache-3.pom" );
        checkPath( paths, "xerces/xercesImpl/2.9.0/xercesImpl-2.9.0.pom" );
        checkPath( paths, "org/apache/ant/ant-dotnet/1.1/ant-dotnet-1.1-ivy.xml" );
    }

    /**
     * Checks if a pom dependency is automatically added to a jar dependency.
     */
    @Test
    public void readPathsPomForJar() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        checkPath( artList.getPaths(), "xml-resolver/xml-resolver/1.2/xml-resolver-1.2.pom" );
    }

    /**
     * Checks if parent is read from the pom and added to the result.
     */
    @Test
    @Ignore
    public void readPathsParent() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        checkPath( artList.getPaths(), "com/redhat/rcm/offliner/repo-parent/1.0.0/repo-parent-1.0.0.pom" );
    }

    /**
     * Checks if imported BOPMs are read from the pom and added to the result without all other managed dependencies.
     */
    @Test
    @Ignore
    public void readPathsImportedBoms() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> paths = artList.getPaths();
        checkPath( paths, "org/jboss/bom/jboss-javaee-6.0-with-all/1.0.7.Final/jboss-javaee-6.0-with-all-1.0.7.Final.pom" );
        checkPath( paths, "commons-lang/commons-lang/2.6/commons-lang-2.6.jar" );
    }

    /**
     * Checks if specified plugins are read from the pom and added to the result.
     */
    @Test
    public void readPathsPlugins() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> paths = artList.getPaths();
        checkPath( paths, "org/apache/maven/plugins/maven-dependency-plugin/2.9/maven-dependency-plugin-2.9.jar" );
        checkPath( paths, "org/apache/maven/plugins/maven-dependency-plugin/2.9/maven-dependency-plugin-2.9.pom" );
    }

    /**
     * Checks if repositories are read from the pom.
     */
    @Test
    public void readPathsRepositories() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> repos = artList.getRepositoryUrls();
        checkRepsoitory( repos, "http://repo1.maven.org/maven2/", true );
        checkRepsoitory( repos, "http://repository.jboss.org/", true );
    }

    /**
     * Checks if a mirror is read from settings.xml and the url of the target repository is replaced correctly.
     */
    @Test
    public void readPathsProcessMirror() throws Exception
    {
        PomArtifactListReader artifactListReader = new PomArtifactListReader( getFile( "settings.xml" ), null );

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> repos = artList.getRepositoryUrls();
        checkRepsoitory( repos, "http://mirror.jboss.org/", true );
        checkRepsoitory( repos, "http://repository.jboss.org/", false );
    }

    /**
     * Checks if type of a dependency is mapped correctly, if its mapping to extension-classifier is defined in the
     * default properties file.
     */
    @Test
    public void readPathsMapType() throws Exception
    {
        PomArtifactListReader artifactListReader = getDefaultListReader();

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> paths = artList.getPaths();
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.pom" );
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.jar" );
        checkPath( paths, "org/apache/ant/ant/1.8.0/ant-1.8.0-tests.jar" );
    }

    /**
     * Checks if type of a dependency is mapped correctly, if its mapping to extension-classifier is defined by an
     * external properties file. First it runs with an empty mapping file to ensure the mapping is not applied. Then the
     * test simply stores the default properties contents into a temporary file and runs checks if mapping was applied.
     */
    @Test
    public void readPathsMapTypeWithExternalMapping() throws Exception
    {
        // create empty mapping properties file
        File mappingFile = getFile( "test.properties" );
        try ( OutputStream os = new FileOutputStream( mappingFile ) )
        {
            // nothing to do, just create an empty file
        }

        // create reader with the empty mapping
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, mappingFile.getPath() );

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> paths = artList.getPaths();
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.pom" );
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.jar", false );
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.maven-plugin" );
        checkPath( paths, "org/apache/ant/ant/1.8.0/ant-1.8.0-tests.jar", false );
        checkPath( paths, "org/apache/ant/ant/1.8.0/ant-1.8.0.test-jar" );

        // read the contents of the internal properties file
        List<String> contents;
        try ( InputStream is = getClass().getClassLoader().getResourceAsStream( PomArtifactListReader.DEFAULT_TYPE_MAPPING_RES ) )
        {
            contents = IOUtils.readLines( is );
        }

        // write the mapping into the external file
        try ( OutputStream os = new FileOutputStream( mappingFile ) )
        {
            IOUtils.writeLines( contents, null, os );
        }

        // create reader with the copied mapping
        artifactListReader = new PomArtifactListReader( null, mappingFile.getPath() );

        artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        paths = artList.getPaths();
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.pom" );
        checkPath( paths, "org/apache/maven/plugins/maven-assembly-plugin/2.5.5/maven-assembly-plugin-2.5.5.jar" );
        checkPath( paths, "org/apache/ant/ant/1.8.0/ant-1.8.0-tests.jar" );
    }


    private void checkPath( final List<String> paths, final String path )
    {
        checkPath( paths, path, true );
    }

    private void checkPath( final List<String> paths, final String path, final boolean present )
    {
        check("path", paths, path, present);
    }

    private void checkRepsoitory( final List<String> repos, final String repo, final boolean present )
    {
        check("repository", repos, repo, present);
    }

    private void check( final String subject, final List<String> heap, final String needle, final boolean present )
    {
        if ( present )
        {
            String msg = StringUtils.capitalize( subject ) + " " + needle + " was not found in the result";
            assertTrue( msg, heap.contains( needle ) );
        }
        else
        {
            assertFalse( "Additional " + subject + " " + needle + " was found in the result", heap.contains( needle ) );
        }
    }

    private PomArtifactListReader getDefaultListReader()
    {
        return new PomArtifactListReader( null, null );
    }

}
