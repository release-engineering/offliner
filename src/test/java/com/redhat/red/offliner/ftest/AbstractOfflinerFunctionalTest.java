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
package com.redhat.red.offliner.ftest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.red.offliner.OfflinerException;
import com.redhat.red.offliner.OfflinerResult;
import com.redhat.red.offliner.alist.io.FoloSerializerModule;
import com.redhat.red.offliner.cli.Main;
import com.redhat.red.offliner.cli.Options;
import com.redhat.red.offliner.ftest.fixture.TestContentGenerator;
import com.redhat.red.offliner.ftest.fixture.TestRepositoryServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by jdcasey on 4/20/16.
 */
public class AbstractOfflinerFunctionalTest
{

    protected List<TestRepositoryServer> repoServers = new ArrayList<>();

    protected TestContentGenerator contentGenerator;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setupFixtures()
            throws IOException
    {
        contentGenerator = new TestContentGenerator();
        objectMapper.registerModule( new FoloSerializerModule() );

        System.out.printf( "START: %s / %s\n", getClass().getSimpleName(), testName.getMethodName() );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "START: {} / {}", getClass().getSimpleName(), testName.getMethodName() );
    }

    @After
    public void stopRepoServers()
    {
        repoServers.forEach( (server) -> server.stop() );

        System.out.printf( "END: %s / %s\n", getClass().getSimpleName(), testName.getMethodName() );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "END: {} / {}", getClass().getSimpleName(), testName.getMethodName() );
    }

    protected TestRepositoryServer newRepositoryServer()
            throws IOException
    {
        TestRepositoryServer server = new TestRepositoryServer(temporaryFolder.newFolder());
        server.start();

        repoServers.add( server );

        return server;
    }

    protected OfflinerResult run( Options options )
            throws IOException, InterruptedException, ExecutionException, OfflinerException
    {
        return new Main( options ).run();
    }

}
