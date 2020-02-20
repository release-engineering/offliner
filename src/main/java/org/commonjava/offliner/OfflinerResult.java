package org.commonjava.offliner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OfflinerResult
{
    private OfflinerRequest request;

    private boolean done;

    private AtomicInteger downloaded = new AtomicInteger( 0 );

    private AtomicInteger avoided = new AtomicInteger( 0 );

    private Map<String, Throwable> errors = new ConcurrentHashMap<>();

    public OfflinerResult( final OfflinerRequest request )
    {
        this.request = request;
    }

    public static OfflinerResult noAction( final OfflinerRequest request )
    {
        OfflinerResult result = new OfflinerResult( request );
        return result.markDone();
    }

    public OfflinerRequest getRequest()
    {
        return request;
    }

    private OfflinerResult markDone()
    {
        this.done = true;
        return this;
    }

    public boolean isDone()
    {
        return done;
    }

    public int getDownloaded()
    {
        return downloaded.get();
    }

    public void addDownloaded( final int downloaded )
    {
        if ( !done )
        {
            this.downloaded.addAndGet( downloaded );
        }
    }

    public int getAvoided()
    {
        return avoided.get();
    }

    public void addAvoided( final int avoided )
    {
        this.avoided.addAndGet( avoided );
    }

    public Map<String, Throwable> getErrors()
    {
        return errors;
    }

    public void putErrors( final Map<String, Throwable> errors )
    {
        this.errors.putAll( errors );
    }

    public void addDownloaded()
    {
        this.downloaded.addAndGet( 1 );
    }

    public void addAvoided()
    {
        this.avoided.addAndGet( 1 );
    }

    public void addError( final String path, final Exception error )
    {
        this.errors.put( path, error );
    }
}
