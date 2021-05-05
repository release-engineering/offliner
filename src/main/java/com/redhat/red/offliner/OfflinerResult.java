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

    private Map<String, String> warns = new ConcurrentHashMap<>();

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

    public Map<String, String> getWarns()
    {
        return warns;
    }

    public void addWarn( final String path, final String warn )
    {
        this.warns.put(path, warn);
    }
}
