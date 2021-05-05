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

import org.apache.commons.codec.binary.Hex;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calculates checksum while the stream it wraps is written to, for later verification.
 */
public final class ChecksumOutputStream
                extends FilterOutputStream
{
    public static final String MD5 = "MD5";

    public static final String SHA_1 = "SHA-1";

    public static final String SHA_256 = "SHA-256";

    private MessageDigest digestMD5;

    private MessageDigest digestSHA1;

    private MessageDigest digestSHA256;

    private Checksum checksum;

    public ChecksumOutputStream( final OutputStream out ) throws NoSuchAlgorithmException
    {
        super( out );
        this.digestMD5 = MessageDigest.getInstance( MD5 );
        this.digestSHA1 = MessageDigest.getInstance( SHA_1 );
        this.digestSHA256 = MessageDigest.getInstance( SHA_256 );
    }

    @Override
    public void write( byte b[], int off, int len ) throws IOException
    {
        if ( ( off | len | ( b.length - ( len + off ) ) | ( off + len ) ) < 0 )
        {
            throw new IndexOutOfBoundsException();
        }
        out.write( b, off, len );
        digestMD5.update( b, off, len );
        digestSHA1.update( b, off, len );
        digestSHA256.update( b, off, len );
    }

    public static final class Checksum
    {
        private String md5;

        private String sha1;

        private String sha256;

        public Checksum( String encodeHexStringMD5, String encodeHexStringSHA1, String encodeHexStringSHA256 )
        {
            this.md5 = encodeHexStringMD5;
            this.sha1 = encodeHexStringSHA1;
            this.sha256 = encodeHexStringSHA256;
        }

        public String getMd5()
        {
            return md5;
        }

        public String getSha1()
        {
            return sha1;
        }

        public String getSha256()
        {
            return sha256;
        }

        @Override
        public String toString()
        {
            return "Checksum{" + "md5='" + md5 + '\'' + ", sha1='" + sha1 + '\'' + ", sha256='" + sha256 + '\'' + '}';
        }

        public boolean isMatch( String encodeHexString )
        {
            if ( encodeHexString == null )
            {
                return false;
            }
            if ( encodeHexString.equalsIgnoreCase( md5 ) )
            {
                return true;
            }
            if ( encodeHexString.equalsIgnoreCase( sha1 ) )
            {
                return true;
            }
            if ( encodeHexString.equalsIgnoreCase( sha256 ) )
            {
                return true;
            }
            return false;
        }
    }

    public Checksum getChecksum()
    {
        if ( checksum == null )
        {
            checksum = new Checksum( Hex.encodeHexString( digestMD5.digest() ),
                                     Hex.encodeHexString( digestSHA1.digest() ),
                                     Hex.encodeHexString( digestSHA256.digest() ) );
        }
        return checksum;
    }
}
