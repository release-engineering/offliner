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
package com.redhat.red.offliner.model;

/**
 * DTO used to contain the result of a download attempt. This captures the path downloaded, the origin URL it was
 * downloaded from, the error (if one occurred), and whether or not the downloaded was avoided because of pre-existing
 * content on the target filesystem.
 *
 * Created by jdcasey on 11/20/15.
 */
public class DownloadResult
{
    private String path;

    private String originUrl;

    private Exception error;

    private boolean avoided;

    public DownloadResult( String originUrl, String path, Exception error, boolean avoided )
    {
        this.originUrl = originUrl;
        this.path = path;
        this.error = error;
        this.avoided = avoided;
    }

    public static DownloadResult success( String originUrl, String path )
    {
        return new DownloadResult( originUrl, path, null, false );
    }

    public static DownloadResult error( String path, Exception error )
    {
        return new DownloadResult( null, path, error, false );
    }

    public static DownloadResult avoid( String path, boolean avoided )
    {
        return new DownloadResult( null, path, null, avoided );
    }

    /**
     * Success means there was no error, and the download was not avoided.
     */
    public boolean isSuccess()
    {
        return error == null && !avoided;
    }

    public boolean isAvoided()
    {
        return avoided;
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
