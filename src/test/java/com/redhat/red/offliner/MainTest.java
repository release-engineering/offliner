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
package com.redhat.red.offliner;

import org.junit.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.redhat.red.offliner.OfflinerUtils.patchPathsForDownload;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 8/16/16.
 */
public class MainTest
{

    @Test
    public void addCorrespondingSha1ChecksumForPOMIfPathsDoesntContainIt()
            throws MalformedURLException
    {
        String pom = "org/foo/bar/1/bar-1.pom";
        String md5 = pom + Offliner.MD5_SUFFIX;
        String sha1 = pom + Offliner.SHA_SUFFIX;

        List<String> src = new ArrayList<>( Arrays.asList( pom, md5 ) );

        assertThat( src.contains( pom ), equalTo( true ) );
        assertThat( src.contains( md5 ), equalTo( true ) );
        assertThat( src.contains( sha1 ), equalTo( false ) );

        patchPathsForDownload( src );

        assertThat( src.contains( pom ), equalTo( true ) );
        assertThat( src.contains( md5 ), equalTo( true ) );
        assertThat( src.contains( sha1 ), equalTo( true ) );
    }

}
