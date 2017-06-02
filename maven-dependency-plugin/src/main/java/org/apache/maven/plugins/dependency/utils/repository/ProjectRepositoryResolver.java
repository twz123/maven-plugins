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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Resolves repositories from the project that's currently being built.
 */
final class ProjectRepositoryResolver
    implements RepositoryResolver
{
    private List<ArtifactRepository> remoteRepositories;

    @Inject
    public ProjectRepositoryResolver( MavenSession session )
    {
        remoteRepositories = getRemoteArtifactRepositories( session );
    }

    @Override
    public List<ArtifactRepository> resolveRepositories()
        throws MojoFailureException
    {
        return remoteRepositories;
    }

    private static List<ArtifactRepository> getRemoteArtifactRepositories( MavenSession session )
    {
        List<ArtifactRepository> repositories = session.getCurrentProject().getRemoteArtifactRepositories();

        if ( repositories == null || repositories.isEmpty() )
        {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList( new ArrayList<ArtifactRepository>( repositories ) );
    }
}
