package com.redhat.rcm.offliner;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.codehaus.plexus.util.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class PomArtifactListReaderTest
{

    private static final String TEMP_DIR = "target/temp";

    @BeforeClass
    public static void prepare() throws IOException
    {
        File tempDir = new File( TEMP_DIR );
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
            InputStream is = PomArtifactListReaderTest.class.getClassLoader().getResourceAsStream( resource );
            File target = new File( TEMP_DIR, resource );
            OutputStream os = new FileOutputStream( target );
            try
            {
                IOUtils.copy( is, os );
            }
            finally
            {
                IOUtils.closeQuietly( is );
                IOUtils.closeQuietly( os );
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
        return new File( TEMP_DIR, filename );
    }

    /**
     * Checks if dependencies are read correctly. It checks if all directly mentioned artifacts are present in the
     * result.
     */
    @Test
    public void readPathsDependencies() throws Exception
    {
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, new BasicCredentialsProvider() );

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
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, new BasicCredentialsProvider() );

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
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, new BasicCredentialsProvider() );

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
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, new BasicCredentialsProvider() );

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
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, new BasicCredentialsProvider() );

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
        PomArtifactListReader artifactListReader = new PomArtifactListReader( null, new BasicCredentialsProvider() );

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
        PomArtifactListReader artifactListReader = new PomArtifactListReader( getFile( "settings.xml" ),
                                                                              new BasicCredentialsProvider() );

        ArtifactList artList = artifactListReader.readPaths( getFile( "repo.pom" ) );

        List<String> repos = artList.getRepositoryUrls();
        checkRepsoitory( repos, "http://mirror.jboss.org/", true );
        checkRepsoitory( repos, "http://repository.jboss.org/", false );
    }

    /**
     * Checks if credentials for a repository is added to the credentials provider.
     */
    @Test
    public void readPathsAddRepositoryCredentials() throws Exception
    {
        BasicCredentialsProvider creds = new BasicCredentialsProvider();
        PomArtifactListReader artifactListReader = new PomArtifactListReader( getFile( "settings.xml" ), creds );

        // call to invoke processing of settings.xml, but the result is not needed
        artifactListReader.readPaths( getFile( "repo.pom" ) );

        Credentials credentials = creds.getCredentials( new AuthScope( "mirror.jboss.org", 80, null, "http" ) );
        assertNotNull( "Credentials for http://mirror.jboss.org/ not loaded", credentials );
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

}
