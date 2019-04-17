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
package com.redhat.red.offliner;

import org.apache.commons.codec.binary.Hex;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calculates a SHA-256 checksum while the stream it wraps is written to, for later verification.
 */
public final class ChecksumOutputStream
        extends FilterOutputStream
{
    private MessageDigest digest;

    public ChecksumOutputStream( final OutputStream out )
            throws NoSuchAlgorithmException
    {
        super( out );
        this.digest = MessageDigest.getInstance( "SHA-256" );
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        out.write(b, off, len);
        digest.update(b, off, len);
    }

    public String getChecksum()
    {
        return Hex.encodeHexString( digest.digest() );
    }
}
