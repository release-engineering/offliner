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
import org.kohsuke.args4j.CmdLineException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.redhat.red.offliner.Offliner.SEPARATING_LINE;
import static com.redhat.red.offliner.OfflinerUtils.processArgsWithHeader;

/**
 * Entry point to Offliner, this class is responsible for orchestrating the entire process.
 */
public class Main
{
    private final Offliner offliner;

    private final Options opts;

    private OfflinerResult result;

    public Main(Options opts)
            throws MalformedURLException
    {
        this.opts = opts;
        this.offliner = new Offliner( OfflinerConfig.builder().fromOptions( opts ).build() );
    }

    public static void main( final String[] args )
    {
        Options opts = new Options();
        boolean start = false;
        try
        {
            start = opts.parseArgs( args );
            opts = processArgsWithHeader( opts );
            start = start && !opts.isHelp();
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
        }
    }

    public OfflinerResult run()
            throws OfflinerException, ExecutionException, InterruptedException, IOException
    {
        this.result = offliner.copyOffline( OfflinerRequest.builder().fromOptions( opts ).build() );

        logErrors();
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
