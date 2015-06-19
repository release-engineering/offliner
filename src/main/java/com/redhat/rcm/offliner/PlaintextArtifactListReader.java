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
package com.redhat.rcm.offliner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

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
    public List<String> readPaths( File file ) throws IOException
    {
        return FileUtils.readLines( file );
    }

    @Override
    public boolean supports( File file )
    {
        String filename = file.getName();
        // TODO think of a better way how to check if the file is supported by this reader
        return !filename.endsWith( ".xml" ) && !filename.endsWith( ".pom" );
    }

}
