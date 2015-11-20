package com.redhat.rcm.offliner.alist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.redhat.rcm.offliner.folo.TrackedContentDTO;
import com.redhat.rcm.offliner.folo.TrackedContentEntryDTO;
import com.redhat.rcm.offliner.folo.io.FoloSerializerModule;
import com.redhat.rcm.offliner.model.ArtifactList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by jdcasey on 11/20/15.
 */
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
                }
            }
        }

        return new ArtifactList( paths, new ArrayList<>( repositories ) );
    }

    private ObjectMapper newObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion( JsonInclude.Include.NON_EMPTY );
        mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true );

        mapper.enable( SerializationFeature.INDENT_OUTPUT, SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID );

        mapper.enable( MapperFeature.AUTO_DETECT_FIELDS );
        //        disable( MapperFeature.AUTO_DETECT_GETTERS );

        mapper.disable( SerializationFeature.WRITE_NULL_MAP_VALUES, SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );

        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );

        mapper.registerModule( new FoloSerializerModule() );

        return mapper;
    }

    @Override
    public boolean supports( File file )
    {
        return file.getName().endsWith( ".json" );
    }
}
