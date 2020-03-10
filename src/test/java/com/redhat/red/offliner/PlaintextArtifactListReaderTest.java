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

import com.redhat.red.offliner.alist.PlaintextArtifactListReader;
import com.redhat.red.offliner.model.ArtifactList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class PlaintextArtifactListReaderTest
{

    private static final String TEMP_DIR = "target/temp";

    private static final String TEMP_PLAINTEXT_DIR = "target/temp/plaintext";

    /**
     * resource0 is an empty csv.
     */
    private static final String RESOURCE0 = "checksum0.csv";

    /**
     * resource1 is a csv file with one column.
     */
    private static final String RESOURCE1 = "checksum1.csv";

    /**
     * resource2 is a csv file with two columns.
     */
    private static final String RESOURCE2 = "checksum2.csv";

    /**
     * resource3 is a csv file with more than two columns.
     */
    private static final String RESOURCE3 = "checksum3.csv";

    private String actualPath = "ghosts/bontee/rotund-amiant/0.6.5/rotund-amiant-0.6.5.jar";

    private String matcherChecksum = "88911386c76a1cb0a3869ce4e53d751a02fe9a2ce38daaa54164c6b82a2b8354";

    @BeforeClass
    public static void prepare()
            throws IOException
    {
        File tempDir = new File( TEMP_PLAINTEXT_DIR );
        if ( tempDir.exists() )
        {
            FileUtils.deleteDirectory( tempDir );
        }
        tempDir.mkdirs();

        List<String> resources = new ArrayList<String>( 4 );
        resources.add( RESOURCE0 );
        resources.add( RESOURCE1 );
        resources.add( RESOURCE2 );
        resources.add( RESOURCE3 );

        for ( String resource : resources )
        {
            File target = new File( TEMP_PLAINTEXT_DIR, resource );
            try (InputStream is = PlaintextArtifactListReaderTest.class.getClassLoader()
                                                                       .getResourceAsStream( resource );
                 OutputStream os = new FileOutputStream( target ))
            {
                if ( resource.equals( RESOURCE0 ) )
                {
                    continue;
                }

                IOUtils.copy( is, os );
            }
        }
    }

    @AfterClass
    public static void cleanup()
            throws IOException
    {
        File tempDir = new File( TEMP_DIR );
        if ( tempDir.exists() )
        {
            FileUtils.deleteDirectory( tempDir );
        }
    }

    @Test
    public void testReadPaths()
            throws Exception
    {
        PlaintextArtifactListReader reader = new PlaintextArtifactListReader();
        File csv0 = FileUtils.getFile( TEMP_PLAINTEXT_DIR, RESOURCE0 );
        File csv1 = FileUtils.getFile( TEMP_PLAINTEXT_DIR, RESOURCE1 );
        File csv2 = FileUtils.getFile( TEMP_PLAINTEXT_DIR, RESOURCE2 );
        File csv3 = FileUtils.getFile( TEMP_PLAINTEXT_DIR, RESOURCE3 );

        ArtifactList artifactList0 = reader.readPaths( csv0 );
        ArtifactList artifactList1 = reader.readPaths( csv1 );
        ArtifactList artifactList2 = reader.readPaths( csv2 );
        ArtifactList artifactList3 = reader.readPaths( csv3 );

        assertNull( "ArctifactList should be null for an empty input file.", artifactList0 );
        assertThat( "Wrong size of checksum map. Should have been 0 for an input file with only one column.",
                    artifactList1.getPaths().contains( actualPath ), equalTo( true ) );
        assertThat( "Wrong checksum value, not matched with the path", artifactList2.getChecksums().get( actualPath ),
                    equalTo( matcherChecksum ) );
        assertThat( "Wrong checksum value, not matched with the path", artifactList3.getChecksums().get( actualPath ),
                    equalTo( matcherChecksum ) );
    }

}
