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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
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

    private final List<RepositoryResolver> resolvers;

    @Inject
    public DependencyRepositoryResolver( Logger logger, ProjectRepositoryResolver projectRepositoryResolver,
                                         PluginParameterRepositoryResolver pluginParamRepositoryResolver )
    {
        this.logger = logger;
        resolvers = Arrays.asList( projectRepositoryResolver, pluginParamRepositoryResolver );
    }

    @Override
    public List<ArtifactRepository> resolveRepositories()
        throws MojoFailureException
    {
        List<ArtifactRepository> resolvedRepositories = new ArrayList<ArtifactRepository>();

        for ( RepositoryResolver resolver : resolvers )
        {
            final List<ArtifactRepository> resolved = resolver.resolveRepositories();
            logger.info( resolver.getClass().getSimpleName() + " resolved " + resolved );
            resolvedRepositories.addAll( resolved );
        }

        return resolvedRepositories;
    }
}