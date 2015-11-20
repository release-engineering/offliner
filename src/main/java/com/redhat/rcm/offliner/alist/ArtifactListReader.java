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
package com.redhat.rcm.offliner.alist;

import com.redhat.rcm.offliner.OfflinerException;
import com.redhat.rcm.offliner.model.ArtifactList;

import java.io.File;
import java.io.IOException;

/**
 * Interface of a paths reader.
 */
public interface ArtifactListReader
{

    /**
     * Reads list of relative paths in a source file.
     *
     * @param file source file
     * @return artifact list containing list of downloadable files along with list of repositories
     */
    ArtifactList readPaths( File file )
            throws IOException, OfflinerException;

    /**
     * CHecks if the given file is supported by this paths reader. The check is performed based on the file contents
     * and/or on the filename. It depends only on the sorce format of a concrete paths reader what is possible to check.
     *
     * @param file checked file
     * @return true is the file is
     */
    boolean supports( File file );

}
