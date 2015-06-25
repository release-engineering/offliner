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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Artifact list paths reader that consumes pom files. It reads all dependencies and constructs paths from them.
 *
 * Supported files are those ending with .pom or named pom.xml.
 */
public class PomArtifactListReader
    implements ArtifactListReader
{

    @Override
    public ArtifactList readPaths( final File file )
        throws IOException
    {
        Reader reader = new FileReader( file );
        Model model;
        try
        {
            model = new MavenXpp3Reader().read( reader );
        }
        catch ( XmlPullParserException ex )
        {
            throw new RuntimeException( "Failed to read the source pom file due to a wrong file contents.", ex);
        }
        finally
        {
            reader.close();
        }

        ArtifactList result = new ArtifactList();
        for ( Dependency dep : model.getDependencies() )
        {
            String path;
            if ( StringUtils.isEmpty( dep.getClassifier() ) )
            {
                path = String.format( "%s/%s/%s/%s-%s.%s", dep.getGroupId().replace( '.', '/' ),
                                      dep.getArtifactId(), dep.getVersion(), dep.getArtifactId(), dep.getVersion(),
                                      dep.getType() );
            }
            else
            {
                path = String.format( "%s/%s/%s/%s-%s-%s.%s", dep.getGroupId().replace( '.', '/' ),
                                      dep.getArtifactId(), dep.getVersion(), dep.getArtifactId(), dep.getVersion(),
                                      dep.getClassifier(), dep.getType() );
            }

            result.addPath( path );
        }

        Map<String, String> repoUrlMap = new LinkedHashMap<>( model.getRepositories().size() );
        for ( Repository repository : model.getRepositories() )
        {
            repoUrlMap.put( repository.getId(), repository.getUrl() );
        }

        // TODO postprocess the repositories based on settings.xml or whatever else

        for ( String repoUrl : repoUrlMap.values() )
        {
            result.addRepositoryUrl( repoUrl );
        }

        return result;
    }

    @Override
    public boolean supports( final File file )
    {
        String filename = file.getName();
        return "pom.xml".equals( filename ) || filename.endsWith( ".pom" );
    }

}
