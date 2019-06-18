package com.redhat.red.offliner;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class ChecksumOutputStreamTest
{
    @Test
    public void run() throws Exception
    {
        Class<?> cls = this.getClass();
        InputStream in = cls.getClassLoader().getResourceAsStream( "repo.pom" );

        ChecksumOutputStream out = new ChecksumOutputStream( new ByteArrayOutputStream() );
        IOUtils.copy( in, out );
        //System.out.println( ">>>" + out.getChecksum() );

        ChecksumOutputStream.Checksum checksum = out.getChecksum();
        assertEquals( checksum.getMd5(), "2572a89c67123db1d5c301f70d506547" );
        assertEquals( checksum.getSha1(), "62aec832ec2c1ffef8c56d38ef871ad6b5a6b3f7" );
        assertEquals( checksum.getSha256(), "3f58003d5ad891dd322d54949ab86011ad13d67dc83885a489b747ffaab36f1f" );
    }
}
