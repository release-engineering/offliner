package org.commonjava.offliner;

import org.commonjava.offliner.cli.Main;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.offliner.model.ArtifactList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.offliner.Offliner.MD5_SUFFIX;
import static org.commonjava.offliner.Offliner.SHA_SUFFIX;

public class OfflinerUtils
{
    /**
     * Sort through the given paths from the {@link ArtifactList} and match up all non-checksum paths to <b>BOTH</b>
     * of its associated checksum paths (sha and md5). If one or both are missing, add them to the paths list.
     *
     * @param paths
     */
    public static void patchPathsForDownload( List<String> paths )
    {
        Logger logger = LoggerFactory.getLogger( OfflinerUtils.class );

        Set<String> nonChecksum = new HashSet<>();
        Map<String, String> pathToSha = new HashMap<>();
        Map<String, String> pathToMd5 = new HashMap<>();
        paths.forEach( (path)->{
            if ( path.endsWith( SHA_SUFFIX ) )
            {
                String base = path.substring( 0, path.length() - SHA_SUFFIX.length() );
                logger.trace( "For base path: '{}', found SHA checksum: '{}'", base, path );
                pathToSha.put( base, path );
            }
            else if ( path.endsWith( MD5_SUFFIX ) )
            {
                String base = path.substring( 0, path.length() - MD5_SUFFIX.length() );
                logger.trace( "For base path: '{}', found MD-5 checksum: '{}'", base, path );
                pathToMd5.put( base, path );
            }
            else
            {
                logger.trace( "Found base path: '{}'", path );
                nonChecksum.add( path );
            }
        } );

        nonChecksum.forEach( ( path ) -> {
            logger.trace( "Checking for checksum paths associated with: '{}'", path );

            logger.trace( "In SHA1 mappings:\n\n{}", pathToSha );
            if ( !pathToSha.containsKey( path ) )
            {
                String sha = path + SHA_SUFFIX;
                logger.trace( "PATCH: Adding sha file: '{}'", sha );
                paths.add( sha );
            }

            logger.trace( "In MD5 mappings:\n\n{}", pathToMd5 );
            if ( !pathToMd5.containsKey( path ) )
            {
                String md5 = path + MD5_SUFFIX;
                logger.trace( "PATCH: Adding md5 file: '{}'", md5 );
                paths.add( md5 );
            }
        } );
    }

    /**
     * Scan the download target directory for Maven POM files, which will be used to generate maven-metadata.xml
     * (Maven repository metadata) files.
     * @param root The download target directory to scan
     * @param pomPaths The list of POM paths collected in previous calls (during the same {@link Main#run()} execution)
     */
    public static void searchForPomPaths( File root, String rootPrefixPath, Set<String> pomPaths )
    {
        if ( null == root || null == pomPaths )
        {
            return;
        }
        if ( root.isDirectory() )
        {
            File[] files = root.listFiles();
            if ( files != null )
            {
                for ( File file : files )
                {
                    searchForPomPaths( file, rootPrefixPath, pomPaths );
                }
            }
        }
        else if ( root.isFile() && root.getName().endsWith( ".pom" ) )
        {
            pomPaths.add( root.getPath().substring( rootPrefixPath.length() + 1 ) );
        }
    }

    /**
     * Given a list of Maven POM files, generate appropriate Maven repository metadata files by parsing the POM's paths
     * and extracting GroupId / ArtifactId / Version information.
     * @param pomPaths List of POM paths to parse
     * @param outputRootPath
     */
    public static void generateMetadata( Set<String> pomPaths, final String outputRootPath )
    {
        Map<ProjectRef, List<SingleVersion>> metas = new HashMap<>();
        for ( String path : pomPaths )
        {
            ArtifactPathInfo artifactPathInfo = ArtifactPathInfo.parse( path );
            ProjectVersionRef gav = artifactPathInfo.getProjectId();
            List<SingleVersion> singleVersions = new ArrayList<SingleVersion>();
            if ( !metas.isEmpty() && metas.containsKey( gav.asProjectRef() ) )
            {
                singleVersions = metas.get( gav.asProjectRef() );
            }
            singleVersions.add( (SingleVersion) gav.getVersionSpec() );
            metas.put( gav.asProjectRef(), singleVersions );
        }
        for ( ProjectRef ga : metas.keySet() )
        {
            List<SingleVersion> singleVersions = metas.get( ga );
            Collections.sort( singleVersions );

            Metadata master = new Metadata();
            master.setGroupId( ga.getGroupId() );
            master.setArtifactId( ga.getArtifactId() );
            Versioning versioning = new Versioning();
            for ( SingleVersion v : singleVersions )
            {
                versioning.addVersion( v.renderStandard() );
            }
            String latest = singleVersions.get( singleVersions.size() - 1 ).renderStandard();
            versioning.setLatest( latest );
            versioning.setRelease( latest );
            master.setVersioning( versioning );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            File metadataFile = Paths.get( outputRootPath,
                                           ga.getGroupId().replace( '.', File.separatorChar ), ga.getArtifactId(),
                                           "maven-metadata.xml" ).toFile();
            try
            {
                new MetadataXpp3Writer().write( baos, master );
                FileUtils.writeByteArrayToFile( metadataFile, baos.toByteArray() );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                System.err.printf( "\n\nFailed to generate maven-metadata file: %s. See above for more information.\n",
                                   metadataFile );
            }
        }
    }

}
