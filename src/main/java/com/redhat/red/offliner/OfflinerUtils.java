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

import com.redhat.red.offliner.cli.Main;
import com.redhat.red.offliner.cli.Options;
import com.redhat.red.offliner.model.ArtifactList;
import io.honeycomb.beeline.tracing.Span;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.commonjava.atlas.maven.ident.version.SingleVersion;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.redhat.red.offliner.cli.Options.HEADER_BREAK_REGEX;
import static com.redhat.red.offliner.cli.Options.HEADER_START;

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
            if ( path.endsWith( Offliner.SHA_SUFFIX ) )
            {
                String base = path.substring( 0, path.length() - Offliner.SHA_SUFFIX.length() );
                logger.trace( "For base path: '{}', found SHA checksum: '{}'", base, path );
                pathToSha.put( base, path );
            }
            else if ( path.endsWith( Offliner.MD5_SUFFIX ) )
            {
                String base = path.substring( 0, path.length() - Offliner.MD5_SUFFIX.length() );
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
                String sha = path + Offliner.SHA_SUFFIX;
                logger.trace( "PATCH: Adding sha file: '{}'", sha );
                paths.add( sha );
            }

            logger.trace( "In MD5 mappings:\n\n{}", pathToMd5 );
            if ( !pathToMd5.containsKey( path ) )
            {
                String md5 = path + Offliner.MD5_SUFFIX;
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

    /**
     * Parse the original arguments to pull the manifests, read their header portions
     * then wrap and append them to be the new arguments.
     * @param args
     * @return
     * @throws CmdLineException
     */
    public static String[] parseArgsWithHeader( String[] args )
            throws CmdLineException
    {
        Options options = new Options();
        options.doParse( args );
        List<String> manifests = options.getLocations();
        if ( manifests == null )
        {
            return args;
        }
        List<String> headerArgs = new ArrayList<>();
        for ( String manifest : manifests )
        {
            File file = new File( manifest );
            List<String> contents = new ArrayList<>();
            try
            {
                contents = FileUtils.readLines( file );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                System.err.println( "Failed to read header in manifest file." );
                System.exit( 1 );
            }
            if ( null == contents || contents.isEmpty() || !contents.get(0).equals( HEADER_START ) )
            {
                continue;
            }
            for ( String c : contents )
            {
                if ( c.equals( HEADER_START ) )
                {
                    continue;
                }
                if ( c.matches( HEADER_BREAK_REGEX ) )
                {
                    break;
                }
                String[] cArr = c.split( "\\s*=\\s*" );
                if ( cArr.length == 0 )
                {
                    continue;
                }
                else if ( cArr.length == 1 )
                {
                    headerArgs.add( "--"+cArr[0] );
                }
                else
                {
                    headerArgs.add( "--"+cArr[0] );
                    headerArgs.add( cArr[1] );
                }
            }
        }
        String[] headerArgsArr = headerArgs.toArray( new String[ headerArgs.size() ] );
        String[] newArgs = (String[]) ArrayUtils.addAll( headerArgsArr, args );
        return newArgs;
    }

    /**
     * Mark the latency metric for honeycomb span
     * @param start
     * @param span
     * @param metric
     */
    public static void markLatency( long start, Span span, String metric )
    {
        if ( span != null )
        {
            long end = System.nanoTime();
            span.addField( metric, end - start );
            span.close();
        }
    }
}
