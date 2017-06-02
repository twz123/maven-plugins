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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Resolves the remote repositories to be used when working with dependencies.
 */
public interface RepositoryResolver
{
    /**
     * @return the repositories to be used, possibly none, but never {@code null}
     * @throws MojoFailureException if repository resolution failed for some reason
     */
    List<ArtifactRepository> resolveRepositories()
        throws MojoFailureException;
}
