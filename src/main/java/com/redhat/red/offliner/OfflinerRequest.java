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

import com.redhat.red.offliner.cli.Options;
import com.redhat.red.offliner.model.ArtifactList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OfflinerRequest
{
    public final static List<String> DEFAULT_URLS = Arrays.asList( Options.DEFAULT_REPO_URL, Options.CENTRAL_REPO_URL  );

    public static Builder builder()
    {
        return new Builder();
    }

    private List<ArtifactList> artifactLists;

    private List<String> artifacttListFiles;

    private File downloadDir;

    private boolean metadataSkipped;

    private List<String> repoUrls;

    private OfflinerRequest( final List<ArtifactList> artifactLists, List<String> artifacttListFiles, File downloadDir,
                             boolean metadataSkipped, final List<String> repoUrls )
    {
        this.artifactLists = artifactLists;
        this.artifacttListFiles = artifacttListFiles;
        this.downloadDir = downloadDir;
        this.metadataSkipped = metadataSkipped;
        this.repoUrls = repoUrls;
    }

    public List<String> getArtifactListFiles()
    {
        return artifacttListFiles;
    }

    public File getDownloadDirectory()
    {
        return downloadDir;
    }

    public boolean isMetadataSkipped()
    {
        return metadataSkipped;
    }

    public List<String> getRepositoryUrls()
    {
        return repoUrls;
    }

    public List<ArtifactList> getArtifactLists()
    {
        return artifactLists;
    }

    public static class Builder
    {
        private List<ArtifactList> artifactLists = new ArrayList<>();

        private List<String> artifactListFiles = new ArrayList<>();

        private File downloadDir;

        private boolean metadataSkipped;

        private List<String> repoUrls = new ArrayList<>();

        private Builder(){}

        public OfflinerRequest build()
        {
            if ( repoUrls == null || repoUrls.isEmpty() )
            {
                repoUrls = DEFAULT_URLS;
            }

            return new OfflinerRequest( artifactLists, artifactListFiles, downloadDir, metadataSkipped, repoUrls );
        }

        public Builder withRepoUrl( String repoUrl )
        {
            this.repoUrls.add( repoUrl );
            return this;
        }

        public Builder withMetadata()
        {
            this.metadataSkipped = false;
            return this;
        }

        public Builder withoutMetadata()
        {
            this.metadataSkipped = true;
            return this;
        }

        public Builder withDownloadDir( File downloadDir )
        {
            this.downloadDir = downloadDir;
            return this;
        }

        public Builder withArtifactList( String artifactList )
        {
            artifactListFiles.add( artifactList );
            return this;
        }

        public Builder withArtifactList( ArtifactList artifactList )
        {
            artifactLists.add( artifactList );
            return this;
        }

        public Builder fromOptions( final Options opts )
        {
            this.artifactListFiles.addAll( opts.getLocations() );
            this.downloadDir = opts.getDownloads();
            this.metadataSkipped = opts.isSkipMetadata();
            this.repoUrls = opts.getBaseUrls();
            return this;
        }
    }
}
