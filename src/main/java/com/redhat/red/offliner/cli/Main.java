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
package com.redhat.red.offliner.cli;

import com.redhat.red.offliner.OfflinerConfig;
import com.redhat.red.offliner.Offliner;
import com.redhat.red.offliner.OfflinerException;
import com.redhat.red.offliner.OfflinerRequest;
import com.redhat.red.offliner.OfflinerResult;
import io.honeycomb.beeline.DefaultBeeline;
import io.honeycomb.beeline.tracing.Span;
import org.kohsuke.args4j.CmdLineException;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static com.redhat.red.offliner.Offliner.*;
import static com.redhat.red.offliner.OfflinerUtils.parseArgsWithHeader;

/**
 * Entry point to Offliner, this class is responsible for orchestrating the entire process.
 */
public class Main
{
    private final Offliner offliner;

    private final Options opts;

    private OfflinerResult result;

    private static DefaultBeeline beeline;

    public static void main( final String[] args )
    {
        Options opts = new Options();
        boolean start = false;
        try
        {
            String[] newArgs = parseArgsWithHeader( args );
            start = opts.parseArgs( newArgs );
        }
        catch ( final CmdLineException e )
        {
            e.printStackTrace();
            System.err.println( "Cannot start. See error output above." );
            System.exit( 1 );
        }

        if ( start )
        {
            try
            {
                loadHoneycombProperties();
                new Main( opts ).run();
            }
            catch ( final MalformedURLException e )
            {
                e.printStackTrace();
                System.err.println( "Cannot initialize HTTP client / context. See error output above." );
                System.exit( 2 );
            }
            catch ( final InterruptedException e )
            {
                System.err.println( "Interrupted waiting for download executor to shutdown." );
            }
            catch ( final ExecutionException e )
            {
                // TODO: Handle suppressed exceptions
                System.err.println( "Download execution manager failed." );
                e.printStackTrace();
            }
            catch ( IOException | OfflinerException e )
            {
                e.printStackTrace();
            }
            finally
            {
                if ( beeline != null )
                {
                    beeline.getBeeline().getTracer().endTrace();
                    beeline.close();
                }
            }
        }
    }

    public Main(Options opts)
            throws MalformedURLException {
        this.opts = opts;
        this.offliner = new Offliner( OfflinerConfig.builder().fromOptions( opts ).build() );
    }

    public OfflinerResult run()
            throws OfflinerException, ExecutionException, InterruptedException, IOException
    {
        long start = System.nanoTime();
        Span rootSpan = beeline == null ? null : beeline.startSpan( "root" );
        this.result = offliner.copyOffline( OfflinerRequest.builder().fromOptions( opts).build(), beeline, rootSpan );

        long startLogErr = System.nanoTime();
        logErrors();
        if ( rootSpan != null )
        {
            long end = System.nanoTime();
            rootSpan.addField( "log_err_ms", ( end - startLogErr ) / NANOS_PER_MILLISECOND );
            rootSpan.addField( "total_timing_ms", ( end - start ) / NANOS_PER_MILLISECOND );
            rootSpan.close();
        }
        return this.result;
    }

    /**
     * Print statistics over System.out and System.err for files downloaded, downloads avoided, and download errors.
     */
    private void logErrors()
    {
        int downloaded = getDownloaded();
        int avoided = getAvoided();
        Map<String, Throwable> errors = getErrors();
        Map<String, String> warns = getWarns();

        System.out.printf( "%d downloads succeeded.\n%d downloads avoided.\n%d downloads warned.\n%d downloads failed.\n\n", downloaded,
                           avoided, warns.size(), errors.size() );

        if ( !errors.isEmpty() )
        {
            System.err.printf( "See %s for details.", Options.ERROR_LOG );

            final File errorLog = new File( Options.ERROR_LOG );
            try (PrintWriter writer = new PrintWriter( new FileWriter( errorLog ) ))
            {
                for ( final Map.Entry<String, Throwable> entry : errors.entrySet() )
                {
                    writer.printf( "Path: %s\n%s\n", entry.getKey(), SEPARATING_LINE );
                    entry.getValue().printStackTrace( writer );
                    writer.printf( "\n%s\n\n", SEPARATING_LINE );
                }
            }
            catch ( final IOException e )
            {
                e.printStackTrace();
                System.err.println( "Failed to write download errors to: " + Options.ERROR_LOG
                                            + ". See above for more information." );
            }
        }
    }

    private static void loadHoneycombProperties()
    {
        String path = System.getProperty( "honeycomb" );
        if ( path != null && !path.trim().isEmpty() )
        {
            Properties properties = new Properties();
            try ( InputStream stream = new FileInputStream( path ) )
            {
                properties.load( stream );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                System.err.println( "Failed to load honeycomb properties." );
                System.exit( 1 );
            }
            System.out.printf( "Reading honeycomb properties, dataset: %s, service: %s.\n", properties.getProperty( HONEYCOMB_DATASET ),
                    properties.getProperty( HONEYCOMB_SERVICE_NAME ) );
            beeline = DefaultBeeline.getInstance( properties.getProperty( HONEYCOMB_DATASET ),
                    properties.getProperty( HONEYCOMB_SERVICE_NAME ),
                    properties.getProperty( HONEYCOMB_WRITE_KEY ) );
        }
    }

    public int getDownloaded()
    {
        return result == null ? 0 : result.getDownloaded();
    }

    public int getAvoided()
    {
        return result == null ? 0 : result.getAvoided();
    }

    public Map<String, Throwable> getErrors()
    {
        return result == null ? Collections.emptyMap() : result.getErrors();
    }

    public Map<String, String> getWarns()
    {
        return result == null ? Collections.emptyMap() : result.getWarns();
    }
}
