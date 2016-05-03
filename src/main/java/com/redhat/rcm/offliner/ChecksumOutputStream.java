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
