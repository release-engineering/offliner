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
