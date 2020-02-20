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
package org.commonjava.offliner.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Utility methods for building URLs from fragments, and for parsing the port out of a URL (or using an appropriate default).
 */
public final class UrlUtils
{

    private UrlUtils()
    {
    }

    /**
     * Given a base URL and some path fragments, build a complete, valid URL
     * @param baseUrl The base URL
     * @param parts the part of the path to append, with path separator characters inserted as appropriate
     * @return The validate, concatenated URL
     * @throws MalformedURLException In case the inputs don't form a valid URL
     */
    public static String buildUrl( final String baseUrl, final String... parts )
        throws MalformedURLException
    {
        if ( parts == null || parts.length < 1 )
        {
            return baseUrl;
        }

        final StringBuilder urlBuilder = new StringBuilder();

        if ( parts[0] == null || !parts[0].startsWith( baseUrl ) )
        {
            urlBuilder.append( baseUrl );
        }

        for ( String part : parts )
        {
            if ( part == null || part.trim()
                                     .length() < 1 )
            {
                continue;
            }

            if ( part.startsWith( "/" ) )
            {
                part = part.substring( 1 );
            }

            if ( urlBuilder.length() > 0 && urlBuilder.charAt( urlBuilder.length() - 1 ) != '/' )
            {
                urlBuilder.append( "/" );
            }

            urlBuilder.append( part );
        }

        return new URL( urlBuilder.toString() ).toExternalForm();
    }

    /**
     * Gets port from the given URL. If no port is specified, then default port is returned.
     *
     * @param url the URL
     * @return port from the given URL or if no port specified, then the default port for the URL protocol
     */
    public static int getPort( URL url )
    {
        int port = url.getPort();
        if ( port == -1 )
        {
            port = url.getDefaultPort();
        }
        return port;
    }

}
