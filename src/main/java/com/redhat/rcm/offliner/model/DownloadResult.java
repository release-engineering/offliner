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
package com.redhat.rcm.offliner.model;

/**
 * Created by jdcasey on 11/20/15.
 */
public class DownloadResult
{
    private String path;

    private String originUrl;

    private Exception error;

    public static DownloadResult success( String originUrl, String path )
    {
        return new DownloadResult( originUrl, path, null );
    }

    public static DownloadResult error( String path, Exception error )
    {
        return new DownloadResult( null, path, error );
    }

    public DownloadResult( String originUrl, String path, Exception error )
    {
        this.originUrl = originUrl;
        this.path = path;
        this.error = error;
    }

    public boolean isSuccess()
    {
        return error == null;
    }

    public String getPath()
    {
        return path;
    }

    public String getOriginUrl()
    {
        return originUrl;
    }

    public Exception getError()
    {
        return error;
    }
}
