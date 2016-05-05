/**
 * Copyright (C) 2015 Red Hat, Inc. (yma@redhat.com)
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

import org.apache.commons.io.IOUtils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public final class ChecksumOutputStream
        extends FilterOutputStream
{

    private final String checksum;

    public ChecksumOutputStream( final OutputStream out, final String checksum )
    {
        super( out );
        this.out = out;
        this.checksum = checksum;
    }

    @Override
    public void close()
            throws IOException
    {
        super.close();

        PrintStream printStream = null;

        try
        {
            printStream = new PrintStream( out );
            printStream.print( checksum );
        }
        finally
        {
            IOUtils.closeQuietly( printStream );
            IOUtils.closeQuietly( out );
        }
    }
}
