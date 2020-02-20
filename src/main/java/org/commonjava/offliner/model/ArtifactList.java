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
package org.commonjava.offliner.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Artifact list DTO. It contains artifact paths list along with the list of repositories where to look for the
 * artifacts.
 */
public class ArtifactList
{

    /**
     * List of repositories where should be artifacts downloaded from. Their order reflects their priority.
     */
    private List<String> repositoryUrls;

    /**
     * List of relative paths of artifacts.
     */
    private List<String> paths;

    /**
     * Map of artifacts relative paths to relevant checksums
     */
    private Map<String, String> checksums;

    public ArtifactList()
    {
        this.paths = new ArrayList<>();
        this.repositoryUrls = new ArrayList<>();
        this.checksums = new HashMap<String, String>();
    }

    public ArtifactList( List<String> paths, List<String> repositories, Map<String, String> checksums )
    {
        this.paths = paths;
        this.repositoryUrls = repositories;
        this.checksums = checksums;
    }


    /**
     * @return the list of repositories where should be artifacts downloaded from. Their order reflects their priority.
     */
    public List<String> getRepositoryUrls()
    {
        return repositoryUrls;
    }

    /**
     * Adds a repository URL to the list of where should be artifacts downloaded from. Their order reflects
     * their priority and so should the order of addRepositoryUrl calls.
     *
     * @param repositoryUrl repository URL to add
     */
    public void addRepositoryUrl( String repositoryUrl )
    {
        repositoryUrls.add( repositoryUrl );
    }

    /**
     * @return the paths
     */
    public List<String> getPaths()
    {
        return paths;
    }

    /**
     * Adds an artifact path to the list.
     *
     * @param path the path to add
     */
    public void addPath( String path )
    {
        if ( !paths.contains( path ) )
        {
            paths.add( path );
        }
    }

    public int size()
    {
        return paths == null ? 0 : paths.size();
    }

    /**
     *
     * @return the Map of artifacts paths to relevant checksums
     */
    public Map<String, String> getChecksums()
    {
        return checksums;
    }
}
