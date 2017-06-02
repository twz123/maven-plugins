package org.apache.maven.plugins.dependency.utils.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;

/**
 * Default {@code RepositoryResolver} implementation that combines the project's repositories and those specified via
 * the <code>${remoteRepositories}</code> plugin parameter.
 */
public class DependencyRepositoryResolver
    implements RepositoryResolver
{
    private final Logger logger;

    private final List<ArtifactRepository> pomRemoteRepositories;

    private final RepositoryResolver pluginParamRepoResolver;

    @Inject
    public DependencyRepositoryResolver( Logger logger, List<ArtifactRepository> pomRemoteRepositories,
                                         @Named( "plugin-parameter" ) RepositoryResolver pluginParamRepoResolver )
    {
        this.logger = logger;
        this.pomRemoteRepositories = pomRemoteRepositories;
        this.pluginParamRepoResolver = pluginParamRepoResolver;
    }

    @Override
    public List<ArtifactRepository> resolveRepositories()
        throws MojoFailureException
    {
        logger.info( "pomRemoteRepositories: " + pomRemoteRepositories );
        logger.info( "pluginParamRepoResolver: " + pluginParamRepoResolver );

        List<ArtifactRepository> resolvedRepositories = new ArrayList<ArtifactRepository>();
        resolvedRepositories.addAll( pomRemoteRepositories );
        resolvedRepositories.addAll( pluginParamRepoResolver.resolveRepositories() );
        return resolvedRepositories;
    }
}
