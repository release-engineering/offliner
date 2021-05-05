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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.copy;
import static org.junit.Assert.assertEquals;

public class ChecksumOutputStreamTest
{
    @Test
    public void run() throws Exception
    {
        Class<?> cls = this.getClass();
        InputStream in = cls.getClassLoader().getResourceAsStream( "repo.pom" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChecksumOutputStream out = new ChecksumOutputStream( baos );
        copy( in, out );

        String md5 = DigestUtils.md5Hex( baos.toByteArray() );
        String sha1 = DigestUtils.sha1Hex( baos.toByteArray() );
        String sha256 = DigestUtils.sha256Hex( baos.toByteArray() );

        //System.out.println( ">>>" + out.getChecksum() );

        ChecksumOutputStream.Checksum checksum = out.getChecksum();
        assertEquals( checksum.getMd5(), md5 );
        assertEquals( checksum.getSha1(), sha1 );
        assertEquals( checksum.getSha256(), sha256 );
    }
}
