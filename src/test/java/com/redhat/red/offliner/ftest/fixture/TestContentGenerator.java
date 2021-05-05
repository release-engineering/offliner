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
package com.redhat.red.offliner.ftest.fixture;

import com.redhat.red.offliner.alist.PlaintextArtifactListReader;
import com.redhat.red.offliner.util.UrlUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Random;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.junit.Assert.fail;

/**
 * This test fixture is responsible for generating a variety of content, ranging from Maven groupId, artifactId, and versions
 * to binary file content. It's all random, but the GAV data is based on a word list (adapted from /usr/src/dict/linux.words on CentOS 7),
 * while binary content is literally a random array of bytes. It will also generate Folo 
 * {@link TrackedContentEntryDTO} records, Maven {@link org.apache.maven.model.Dependency} instances,
 * and checksums for use as part of an input file for the {@link PlaintextArtifactListReader}.
 * 
 * Created by jdcasey on 4/20/16.
 */
public class TestContentGenerator
{
    public static final int DEFAULT_GROUP_ID_SEGMENTS = 2;

    public static final int DEFAULT_ARTIFACT_ID_SEGMENTS = 2;

    public static final int DEFAULT_VERSION_SEGMENTS = 3;

    private final List<String> words;

    private Random random = new Random();

    public TestContentGenerator()
            throws IOException
    {
        words = IOUtils.readLines( Thread.currentThread().getContextClassLoader().getResourceAsStream( "words.txt" ) );
    }

    public Model newPom()
    {
        String g = newGroupId();
        String a = newArtifactId();
        String v = newVersion();

        Model m = new Model();
        m.setModelVersion( "4.0.0" );
        m.setGroupId( g );
        m.setArtifactId( a );
        m.setVersion( v );

        return m;
    }

    public Model newPomFor( Dependency dep )
    {
        Model m = new Model();
        m.setModelVersion( "4.0.0" );
        m.setGroupId( dep.getGroupId() );
        m.setArtifactId( dep.getArtifactId() );
        m.setVersion( dep.getVersion() );

        return m;
    }

    public Model newPomWithExternalParent()
    {
        String g = newGroupId();
        String a = newArtifactId();
        String v = newVersion();

        Model m = new Model();
        m.setModelVersion( "4.0.0" );
        m.setGroupId( g );
        m.setArtifactId( a );
        m.setVersion( v );

        String pg = newGroupId();
        String pa = newArtifactId();
        String pv = newVersion();

        Parent p = new Parent();
        p.setGroupId( pg );
        p.setArtifactId( pa );
        p.setVersion( pv );

        return m;
    }

    public Model newModulePom( String groupId, String version )
    {
        String pa = newArtifactId();

        String a = newArtifactId();

        Model m = new Model();
        m.setModelVersion( "4.0.0" );
        m.setArtifactId( a );

        Parent p = new Parent();
        p.setGroupId( groupId );
        p.setArtifactId( pa );
        p.setVersion( version );

        return m;
    }

    public Dependency newDependency( String type, String classifier, String scope )
    {
        String g = newGroupId();
        String a = newArtifactId();
        String v = newVersion();
        Dependency d = new Dependency();
        d.setGroupId( g );
        d.setArtifactId( a );
        d.setVersion( v );

        if ( isNotEmpty( classifier ) )
        {
            d.setClassifier( classifier );
        }

        if ( isNotEmpty( type ) && !"jar".equalsIgnoreCase( type ) )
        {
            d.setType( type );
        }

        if ( isNotEmpty( scope ) )
        {
            d.setScope( scope );
        }

        return d;
    }

    public Dependency newDependency()
    {
        return newDependency( null, null, null );
    }

    public Dependency newDependency( String type )
    {
        return newDependency( type, null, null );
    }

    public Dependency newDependency( String type, String scope )
    {
        return newDependency( type, null, scope );
    }

    public Repository newRepository( String id, String url )
    {
        Repository repo = new Repository();
        repo.setId( id );
        repo.setUrl( url );

        RepositoryPolicy releasesPolicy = new RepositoryPolicy();
        RepositoryPolicy snapshotsPolicy = new RepositoryPolicy();

        releasesPolicy.setEnabled( true );
        snapshotsPolicy.setEnabled( true );

        repo.setReleases( releasesPolicy );
        repo.setSnapshots( snapshotsPolicy );

        return repo;

    }

    public TrackedContentEntryDTO newRemoteContentEntry( StoreKey key, String type, String originBaseUri, byte[] content )
            throws MalformedURLException
    {
        String artifactPath = newArtifactPath( type );
        TrackedContentEntryDTO dto = new TrackedContentEntryDTO( key, AccessChannel.NATIVE, artifactPath );
        dto.setMd5( md5Hex( content ) );
        dto.setSha256( sha256Hex( content ) );
        dto.setOriginUrl( UrlUtils.buildUrl( originBaseUri, artifactPath ) );

        return dto;
    }

    public TrackedContentEntryDTO newRemoteContentEntry( StoreKey key, String type, String classifier, String originBaseUri, byte[] content )
            throws MalformedURLException
    {
        String artifactPath = newArtifactPath( type, classifier );
        TrackedContentEntryDTO dto = new TrackedContentEntryDTO( key, AccessChannel.NATIVE, artifactPath );
        dto.setMd5( md5Hex( content ) );
        dto.setSha256( sha256Hex( content ) );
        dto.setOriginUrl( UrlUtils.buildUrl( originBaseUri, artifactPath ) );

        return dto;
    }

    public TrackedContentEntryDTO newHostedContentEntry( StoreKey key, String type, String localBaseUri, byte[] content )
            throws MalformedURLException
    {
        String artifactPath = newArtifactPath( type );
        TrackedContentEntryDTO dto = new TrackedContentEntryDTO( key, AccessChannel.NATIVE, artifactPath );
        dto.setMd5( md5Hex( content ) );
        dto.setSha256( sha256Hex( content ) );
        dto.setOriginUrl( UrlUtils.buildUrl( localBaseUri, artifactPath ) );

        return dto;
    }

    public TrackedContentEntryDTO newHostedContentEntry( StoreKey key, String type, String classifier, String localBaseUri, byte[] content )
            throws MalformedURLException
    {
        String artifactPath = newArtifactPath( type, classifier );
        TrackedContentEntryDTO dto = new TrackedContentEntryDTO( key, AccessChannel.NATIVE, artifactPath );
        dto.setMd5( md5Hex( content ) );
        dto.setSha256( sha256Hex( content ) );
        dto.setOriginUrl( UrlUtils.buildUrl( localBaseUri, artifactPath ) );

        return dto;
    }

    public String newPlaintextEntryWithChecksum( String path, byte[] content )
    {
        return String.format( "%s,%s", sha256Hex( content ), path );
    }

    public String newPlaintextEntryWithoutChecksum( String path )
    {
        return String.format( ",%s", path );
    }

    public String newNakedPlaintextEntry( String path )
    {
        return path;
    }

    public byte[] newBinaryContent( int size )
    {
        byte[] data = new byte[size];
        random.nextBytes( data );
        return data;
    }

    public String newArtifactPath( String type )
    {
        return newArtifactPath( type, null, DEFAULT_GROUP_ID_SEGMENTS, DEFAULT_ARTIFACT_ID_SEGMENTS,
                                DEFAULT_VERSION_SEGMENTS );
    }

    public String newArtifactPath( String type, String classifier )
    {
        return newArtifactPath( type, classifier, DEFAULT_GROUP_ID_SEGMENTS, DEFAULT_ARTIFACT_ID_SEGMENTS,
                                DEFAULT_VERSION_SEGMENTS );
    }

    public String newArtifactPath( String type, String classifier, int groupSize, int artifactSize, int versionSize )
    {
        StringBuilder sb = new StringBuilder();
        String artifactId = newArtifactId( artifactSize );
        String version = newVersion( versionSize );

        sb.append( newGroupPath( groupSize ) )
          .append( "/" )
          .append( artifactId )
          .append( "/" )
          .append( version )
          .append( "/" )
          .append( artifactId )
          .append( "-" )
          .append( version );

        if ( isNotEmpty( classifier) )
        {
            sb.append( "-" ).append( classifier );
        }

        sb.append( "." )
          .append( type );

        return sb.toString();
    }

    public String newGroupId()
    {
        return newGroupId( DEFAULT_GROUP_ID_SEGMENTS );
    }

    public String newGroupId( int size )
    {
        return newStringOfWords( size, "." );
    }

    public String newGroupPath()
    {
        return newGroupPath( DEFAULT_GROUP_ID_SEGMENTS );
    }

    public String newGroupPath( int size )
    {
        return newStringOfWords( size, "/" );
    }

    public String newArtifactId()
    {
        return newArtifactId( DEFAULT_ARTIFACT_ID_SEGMENTS );
    }

    public String newArtifactId( int size )
    {
        return newStringOfWords( size, "-" );
    }

    public String newVersion()
    {
        return newVersion( DEFAULT_VERSION_SEGMENTS );
    }

    public String newVersion( int size )
    {
        return newStringOfIntegers( 1, size, "." );
    }

    public String newStringOfIntegers( int maxDigits, int size, String joint )
    {
        StringBuilder sb = new StringBuilder();
        for( int i=0; i<size; i++)
        {
            if ( sb.length() > 0 )
            {
                sb.append( joint );
            }

            sb.append( Integer.toString( Math.abs( random.nextInt() ) % ( maxDigits * 10 ) ) );
        }

        return sb.toString();
    }

    public String newStringOfWords( int size, String joint )
    {
        StringBuilder sb = new StringBuilder();
        for( int i=0; i<size; i++)
        {
            if ( sb.length() > 0 )
            {
                sb.append( joint );
            }

            sb.append( words.get( Math.abs( random.nextInt() ) % words.size() ) );
        }

        return sb.toString();
    }

    public String pathOf( Dependency dep )
    {
        StringBuilder sb = new StringBuilder();
        String a = dep.getArtifactId();
        String v = dep.getVersion();

        sb.append( dep.getGroupId().replace( '.', '/' ) ).append('/').append(a).append('/').append(v).append('/').append(a).append('-').append(v);

        if ( isNotEmpty( dep.getClassifier() ) )
        {
            sb.append( '-' ).append( dep.getClassifier() );
        }

        sb.append( '.' );

        String t = dep.getType();
        if ( isEmpty( t ) )
        {
            sb.append( "jar" );
        }
        else
        {
            sb.append( t );
        }

        return sb.toString();
    }

    public String pathOf( Model pomDep )
    {
        StringBuilder sb = new StringBuilder();
        String g = pomDep.getGroupId();
        String a = pomDep.getArtifactId();
        String v = pomDep.getVersion();

        Parent parent = pomDep.getParent();
        if ( isEmpty( g ) )
        {
            if ( parent == null )
            {
                fail( "No groupId or parent declaration in POM!" );
            }
            else
            {
                g = parent.getGroupId();
            }
        }

        if ( isEmpty( v ) )
        {
            if ( parent == null )
            {
                fail( "No version or parent declaration in POM!" );
            }
            else
            {
                v = parent.getVersion();
            }
        }

        sb.append( g.replace( '.', '/' ) ).append('/').append(a).append('/').append(v).append('/').append(a).append('-').append(v);

        sb.append( ".pom" );

        return sb.toString();
    }

    public String pomToString( Model pom )
            throws IOException
    {
        StringWriter sw = new StringWriter();
        new MavenXpp3Writer().write( sw, pom );

        return sw.toString();
    }
}
