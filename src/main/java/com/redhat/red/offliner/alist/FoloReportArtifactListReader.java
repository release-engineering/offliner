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
package com.redhat.red.offliner.alist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.redhat.red.offliner.alist.io.FoloSerializerModule;
import com.redhat.red.offliner.model.ArtifactList;
import org.apache.commons.io.FileUtils;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link ArtifactListReader} implementation that parses JSON report files from the Indy Maven repository manager's Folo
 * add-on, which does content activity tracking on a per-session basis. See:
 * <a href="https://commonjava.github.io/indy/user/addons/folo/index.html">https://commonjava.github.io/indy/user/addons/folo/index.html</a>
 * Created by jdcasey on 11/20/15.
 */
@Deprecated
public class FoloReportArtifactListReader
    implements ArtifactListReader
{
    @Override
    public ArtifactList readPaths( File file )
            throws IOException
    {
        String json = FileUtils.readFileToString( file );
        ObjectMapper mapper = newObjectMapper();

        TrackedContentDTO report = mapper.readValue( json, TrackedContentDTO.class );
        Set<TrackedContentEntryDTO> downloads = report.getDownloads();

        Set<String> repositories = new HashSet<>();
        List<String> paths = new ArrayList<>();
        Map<String, String> checksums = new HashMap<String, String>();
        if ( downloads != null )
        {
            for ( TrackedContentEntryDTO download : downloads )
            {
                String path = download.getPath();
                if ( path.contains( "maven-metadata.xml" ) )
                {
                    continue;
                }

                String url = download.getOriginUrl();
                if ( url == null )
                {
                    url = download.getLocalUrl();
                }

                if ( url != null )
                {
                    paths.add( path );
                    repositories.add( url.substring( 0, url.length() - path.length() ) );
                    String checksum = download.getSha256();
                    if ( null != checksum && !checksum.isEmpty() )
                    {
                        checksums.put( path, checksum );
                    }
                }
            }
        }

        return new ArtifactList( paths, new ArrayList<>( repositories ), checksums );
    }

    private ObjectMapper newObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion( JsonInclude.Include.NON_EMPTY );
        mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true );

        mapper.enable( SerializationFeature.INDENT_OUTPUT, SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID );

        mapper.enable( MapperFeature.AUTO_DETECT_FIELDS );

        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );

        mapper.registerModule( new FoloSerializerModule() );

        return mapper;
    }

    @Override
    public boolean supports( File file )
    {
        boolean result = file.getName().endsWith( ".json" );
        if ( result )
        {
            System.out.println( "WARN: Folo manifest-file format is deprecated and will be removed in future." );
        }
        return result;
    }
}
