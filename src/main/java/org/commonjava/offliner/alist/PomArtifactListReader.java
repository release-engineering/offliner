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
package org.commonjava.offliner.alist;

import org.commonjava.offliner.OfflinerException;
import org.commonjava.offliner.model.ArtifactList;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.DefaultMirrorSelector;
import org.apache.maven.repository.MirrorSelector;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Artifact list paths reader that consumes pom files. It reads all dependencies and constructs paths from them.
 *
 * Supported files are those ending with .pom or named pom.xml.
 */
public class PomArtifactListReader
    implements ArtifactListReader
{

    public static final String DEFAULT_TYPE_MAPPING_RES = "type-mapping.properties";

    private File settingsXml;

    private Map<String, TypeMapping> typeMapping;

    public PomArtifactListReader( final File settingsXml, final String typeMappingFile  )
    {
        this.settingsXml = settingsXml;

        Properties props = new Properties();
        if ( StringUtils.isEmpty( typeMappingFile ) )
        {
            try ( InputStream mappingStream = getClass().getClassLoader().getResourceAsStream( DEFAULT_TYPE_MAPPING_RES ) )
            {
                props.load( mappingStream );
            }
            catch ( IOException ex )
            {
                throw new IllegalStateException( "Failed to load Maven type mapping from default properties", ex );
            }
        }
        else
        {
            try ( InputStream mappingStream = new FileInputStream( typeMappingFile ) )
            {
                props.load( mappingStream );
            }
            catch ( IOException ex )
            {
                throw new IllegalStateException( "Failed to load Maven type mapping provided properties file "
                                                 + typeMappingFile, ex );
            }
        }
        this.typeMapping = new HashMap<>( props.size() );

        Pattern p = Pattern.compile( "([^:]+)(?::(.+))?" );
        for ( Map.Entry<Object, Object> entry : props.entrySet() )
        {
            String type = (String) entry.getKey();

            String value = (String) entry.getValue();
            Matcher m = p.matcher( value );
            if ( ! m.matches() )
            {
                throw new IllegalArgumentException( "The type mapping string \"" + typeMappingFile
                                                    + "\" has a wrong format." );
            }
            String extension = m.group( 1 );
            if ( m.groupCount() == 2 )
            {
                String classifier = m.group( 2 );
                this.typeMapping.put( type, new TypeMapping( extension, classifier ) );
            }
            else
            {
                this.typeMapping.put( type, new TypeMapping( extension ) );
            }
        }
    }


    @Override
    public ArtifactList readPaths( final File file )
            throws IOException, OfflinerException
    {
        Model model;
        try (Reader reader = new FileReader( file ))
        {
            model = new MavenXpp3Reader().read( reader );
        }
        catch ( XmlPullParserException ex )
        {
            throw new OfflinerException( "Failed to parse the source pom: %s. Invalid XML: %s", ex, file,
                                         ex.getMessage() );
        }

        Set<String> paths = new LinkedHashSet<>();
        for ( Dependency dep : model.getDependencies() )
        {
            String dirPath = String.format( "%s/%s/%s", dep.getGroupId().replace( '.', '/' ), dep.getArtifactId(),
                                            dep.getVersion() );
            String extension = dep.getType();
            String classifier = dep.getClassifier();
            if ( typeMapping.containsKey( extension ) )
            {
                TypeMapping tm = typeMapping.get( extension );
                extension = tm.getExtension();
                classifier = tm.getClassifier();
            }

            if ( !"pom".equals( extension ))
            {
                paths.add( String.format( "%s/%s-%s.pom", dirPath, dep.getArtifactId(), dep.getVersion() ) );
            }

            String path;
            if ( StringUtils.isEmpty( classifier ) )
            {
                path = String.format( "%s/%s-%s.%s", dirPath, dep.getArtifactId(), dep.getVersion(), extension );
            }
            else
            {
                path = String.format( "%s/%s-%s-%s.%s", dirPath, dep.getArtifactId(), dep.getVersion(),
                                      classifier, extension );
            }

            paths.add( path );
        }

        if ( model.getBuild() != null )
        {
            List<Plugin> plugins = model.getBuild().getPlugins();
            for ( Plugin dep : plugins )
            {
                String prefix = String.format( "%s/%s/%s/%s-%s.", dep.getGroupId().replace( '.', '/' ),
                                               dep.getArtifactId(), dep.getVersion(),dep.getArtifactId(),
                                               dep.getVersion() );
                paths.add( String.format( "%spom", prefix ) );
                paths.add( String.format( "%sjar", prefix ) );
            }
        }

        List<String> repoUrls = new ArrayList<>();

        List<Repository> repositories = model.getRepositories();
        if ( repositories != null )
        {
            processSettingsXml( repositories );

            for ( Repository repository : repositories )
            {
                repoUrls.add( repository.getUrl() );
            }

        }

        ArtifactList result = new ArtifactList( new ArrayList<String>( paths ), repoUrls, null );
        return result;
    }

    /**
     * Processes informations contained in provided settings.xml. Uses repository URLs from speicifed mirrors and uses
     * the authentication info cantained in servers section.
     *
     * @param repositories the repository list
     * @throws IOException in case the settings.xml file cannot be found or read
     */
    private void processSettingsXml( List<Repository> repositories )
            throws IOException, OfflinerException
    {
        if ( settingsXml != null )
        {
            Settings settings;
            try (Reader reader = new FileReader( settingsXml ))
            {
                settings = new SettingsXpp3Reader().read( reader );
            }
            catch ( XmlPullParserException ex )
            {
                throw new OfflinerException( "Failed to parse: %s. Invalid XML: %s", ex, settingsXml, ex.getMessage() );
            }

            processMirrors( settings, repositories );
            processCredentials( settings, repositories );
        }
    }

    /**
     * Applies mirrors from the settings.xml on the {@code repositories}. Read mirrors replace the original repositories
     * in provided repository list.
     *
     * @param settings settings.xml contents
     * @param repositories list of repositories read from pom
     */
    private void processMirrors( final Settings settings, final List<Repository> repositories )
    {
        List<Mirror> mirrors = settings.getMirrors();
        MirrorSelector mirrorSelector = new DefaultMirrorSelector();
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        for ( Repository repository : new ArrayList<>( repositories ) )
        {
            ArtifactRepository artRepository = new MavenArtifactRepository();
            artRepository.setId( repository.getId() );
            // TODO read the layout from the original repository
            artRepository.setLayout( layout );
            Mirror mirror = mirrorSelector.getMirror( artRepository, mirrors );
            if ( mirror != null )
            {
                Repository mirrorRepository = new Repository();
                mirrorRepository.setId( mirror.getId() );
                mirrorRepository.setLayout( mirror.getLayout() );
                mirrorRepository.setReleases( repository.getReleases() );
                mirrorRepository.setSnapshots( repository.getSnapshots() );
                mirrorRepository.setUrl( mirror.getUrl() );
                Collections.replaceAll( repositories, repository, mirrorRepository );
            }
        }
    }

    /**
     * Read server credentials from the settings.xml. Reads only credentials for servers contained in the
     * {@code repositories}.
     *
     * @param settings settings.xml contents
     * @param repositories the repository list
     */
    private void processCredentials( final Settings settings, final List<Repository> repositories )
            throws OfflinerException
    {
        Map<String, Repository> repoMap = new HashMap<>();
        for ( Repository repository : repositories )
        {
            repoMap.put( repository.getId(), repository );
        }

        List<Server> servers = settings.getServers();
        for ( Server server : servers )
        {
            if ( repoMap.containsKey( server.getId() ) )
            {
                String username = server.getUsername();
                Credentials credentials = null;
                if ( username != null )
                {
                    credentials = new UsernamePasswordCredentials( username, server.getPassword() );
                }
            }
        }
    }

    @Override
    public boolean supports( final File file )
    {
        String filename = file.getName();
        return "pom.xml".equals( filename ) || filename.endsWith( ".pom" );
    }

    private static class TypeMapping
    {

        private final String extension;

        private final String classifier;


        public TypeMapping( final String extension, final String classifer )
        {
            this.extension = extension;
            this.classifier = classifer;
        }

        public TypeMapping( final String extension )
        {
            this( extension, null );
        }


        public String getExtension()
        {
            return extension;
        }

        public String getClassifier()
        {
            return classifier;
        }

    }

}
