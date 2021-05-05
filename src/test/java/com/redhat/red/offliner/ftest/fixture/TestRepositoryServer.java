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
package com.redhat.red.offliner.ftest.fixture;

import org.commonjava.test.http.stream.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;

/**
 * This test fixture manages an HTTP server instance that serves files from a directory structure. It supports writing
 * files into this dir structure, which sets expectations. Files that don't exist should result in a 404 response.
 *
 * Created by jdcasey on 4/20/16.
 */
public class TestRepositoryServer
{
    // TODO: This doesn't allow registering network- or server-response error expectations. We need to port this capability from ExpectationServer.
    private StreamServer server;

    private File basedir;

    public TestRepositoryServer( File basedir )
    {
        this.basedir = basedir;
        server = new StreamServer( ( path ) -> new FileInputStream( new File( basedir, path ) ) );
    }

    public void registerContent( String path, byte[] data )
            throws IOException
    {
        File f = new File( basedir, path );
        System.out.printf( "Writing: %s for path: %s on server: %s\n", f, path, server.getBaseUri() );
        writeByteArrayToFile( f, data );
    }

    public void registerContent( String path, String data )
            throws IOException
    {
        File f = new File( basedir, path );
        System.out.printf( "Writing: %s for path: %s on server: %s\n", f, path, server.getBaseUri() );
        write( f, data );
    }

    public void stop()
    {
        server.stop();
    }

    public StreamServer start()
            throws IOException
    {
        return server.start();
    }

    public String getBaseUri()
    {
        return server.getBaseUri();
    }

    public String formatUrl( String... subpath )
    {
        return server.formatUrl( subpath );
    }

    public String formatPath( String... subpath )
    {
        return server.formatPath( subpath );
    }

    public int getPort()
    {
        return server.getPort();
    }

    public Integer getAccessesFor( String path )
    {
        return server.getAccessesFor( path );
    }

    public Integer getAccessesFor( String method, String path )
    {
        return server.getAccessesFor( method, path );
    }

    public Map<String, Integer> getAccessesByPathKey()
    {
        return server.getAccessesByPathKey();
    }
}
