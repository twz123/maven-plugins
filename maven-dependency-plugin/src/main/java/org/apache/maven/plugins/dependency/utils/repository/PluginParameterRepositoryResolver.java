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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * Resolves repositories from the parameter {@value #REMOTE_REPOSITORIES_PARAM_NAME}.
 * <p>
 * Repositories in the format {@code id::[layout]::url} or just {@code url}, separated by comma. This is the same format
 * that's used by the Maven Deploy Plugin.
 * <p>
 * Example: <br/>
 * {@code central::default::http://repo1.maven.apache.org/maven2,myrepo::::http://repo.acme.com,http://repo.acme2.com}
 */
final class PluginParameterRepositoryResolver
    implements RepositoryResolver
{
    private static final String REMOTE_REPOSITORIES_PARAM_NAME = "${remoteRepositories}";

    private static final Pattern REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.*)::(.+)" );

    private final Logger logger;

    /**
     * Used to lookup the remote repositories configuration.
     */
    private final TypeAwareExpressionEvaluator evaluator;

    /**
     * Map that contains the layouts.
     */
    private final Map<String, ArtifactRepositoryLayout> repositoryLayouts;

    @Inject
    public PluginParameterRepositoryResolver( Logger logger, MavenSession session, MojoExecution mojoExecution,
                                              Map<String, ArtifactRepositoryLayout> repositoryLayouts )
    {
        this( logger, new PluginParameterExpressionEvaluator( session, mojoExecution ), repositoryLayouts );
    }

    PluginParameterRepositoryResolver( Logger logger, TypeAwareExpressionEvaluator evaluator,
                                       Map<String, ArtifactRepositoryLayout> repositoryLayouts )
    {
        this.logger = logger;
        this.evaluator = evaluator;
        this.repositoryLayouts = repositoryLayouts;
    }

    @Override
    public List<ArtifactRepository> resolveRepositories()
        throws MojoFailureException
    {
        String remoteRepositories = getRemoteRepositoriesParameter();

        logger.info( "remoteRepositories: " + remoteRepositories );
        logger.info( "repositoryLayouts: " + repositoryLayouts );

        if ( remoteRepositories == null )
        {
            return Collections.emptyList();
        }
        else
        {
            List<ArtifactRepository> resolvedRepositories = new ArrayList<ArtifactRepository>();

            ArtifactRepositoryPolicy always =
                new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                              ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );

            for ( String remoteRepository : StringUtils.split( remoteRepositories, "," ) )
            {
                resolvedRepositories.add( parseRepository( remoteRepository, always ) );
            }

            logger.info( "resolvedRepositories: " + resolvedRepositories );
            return resolvedRepositories;
        }
    }

    private String getRemoteRepositoriesParameter()
        throws MojoFailureException
    {
        Exception cause;

        try
        {
            return (String) evaluator.evaluate( REMOTE_REPOSITORIES_PARAM_NAME );
        }
        catch ( ClassCastException e )
        {
            cause = e;
        }
        catch ( ExpressionEvaluationException e )
        {
            cause = e;
        }

        String message = "Failed to resolve remote repositories";
        throw (MojoFailureException) new MojoFailureException( REMOTE_REPOSITORIES_PARAM_NAME, message,
                                                               message + ": " + cause.getMessage() ).initCause( cause );
    }

    ArtifactRepository parseRepository( String repository, ArtifactRepositoryPolicy policy )
        throws MojoFailureException
    {
        String id, url;
        ArtifactRepositoryLayout layout = null;

        if ( repository.contains( "::" ) ) // if it's an extended repo URL of the form id::layout::urlF
        {
            Matcher matcher = REPO_SYNTAX_PATTERN.matcher( repository );
            if ( !matcher.matches() )
            {
                throw new MojoFailureException( repository, "Invalid syntax for repository: " + repository,
                                                "Invalid syntax for repository. Use \"id::layout::url\" or \"URL\"." );
            }

            id = matcher.group( 1 ).trim();
            if ( !StringUtils.isEmpty( matcher.group( 2 ) ) )
            {
                layout = getLayout( matcher.group( 2 ).trim() );
            }
            url = matcher.group( 3 ).trim();
        }
        else // it's a simple url
        {
            id = "temp";
            url = repository;
        }

        if ( layout == null )
        {
            layout = getLayout( "default" );
        }

        return new MavenArtifactRepository( id, url, layout, policy, policy );
    }

    private ArtifactRepositoryLayout getLayout( String id )
        throws MojoFailureException
    {
        ArtifactRepositoryLayout layout = repositoryLayouts.get( id );

        if ( layout == null )
        {
            throw new MojoFailureException( id, "Invalid repository layout", "Invalid repository layout: " + id );
        }

        return layout;
    }
}
