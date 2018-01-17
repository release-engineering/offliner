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
package com.redhat.red.offliner.alist;

import com.redhat.red.offliner.model.ArtifactList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Artifact list paths reader which reads the paths from a plaintext file. On each line a relative path to the
 * repository root is expected. The reader simply reads the lines and returns them. The lines are not trimmed in any way
 * and empty lines are returned along with standard lines which can cause subsequent problems.
 *
 * Supported files cannot end with .xml or .pom. This is simply to exclude pom files and may be changed in the future.
 */
public class PlaintextArtifactListReader implements ArtifactListReader
{

    @Override
    public ArtifactList readPaths( final File file ) throws IOException
    {
        List<String> paths = new ArrayList<>();
        Map<String, String> checksums = new HashMap<String, String>();
        List<String> contents = FileUtils.readLines( file );

        if ( null == contents || contents.isEmpty() )
        {
            return null;
        }

        for ( String c : contents )
        {
            c = c.trim();
            if ( c.startsWith( "#" ) || c.startsWith( "//" ) || c.startsWith( ";" ) )
            {
                //common comment types.
                continue;
            }

            // handle potential for spaces around the comma.
            String[] cArr = c.split( "\\s*,\\s*" );
            if ( cArr.length == 0 )
            {
                continue;
            }
            else if ( cArr.length == 1 )
            {
                paths.add(cArr[0]);
            }
            else
            {
                paths.add( cArr[1] );
                checksums.put( cArr[1], cArr[0] );
            }
        }

        ArtifactList result = new ArtifactList( paths, Collections.emptyList(), checksums );
        return result;
    }

    @Override
    public boolean supports( final File file )
    {
        String filename = file.getName();
        // TODO think of a better way how to check if the file is supported by this reader
        return !filename.endsWith(".json") && !filename.endsWith( ".xml" ) && !filename.endsWith( ".pom" );
    }
}
