package org.apache.maven.plugins.dependency;

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

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.utils.repository.RepositoryResolver;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.ArtifactCoordinate;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.DependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Resolves a single artifact, eventually transitively, from the specified remote repositories.
 * Caveat: will always check the central repository defined in the super pom. You could use a mirror entry in your
 * <code>settings.xml</code>
 */
@Mojo( name = "get", requiresProject = false, threadSafe = true )
public class GetMojo
    extends AbstractMojo
{
    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    private MavenSession session;
    
    /**
     *
     */
    @Component
    private ArtifactResolver artifactResolver;

    /**
    *
    */
   @Component
   private DependencyResolver dependencyResolver;

   @Component
   private ArtifactHandlerManager artifactHandlerManager;

    @Component
    private RepositoryResolver repositoryResolver;

    private DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
    
    /**
     * The groupId of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "groupId" )
    private String groupId;

    /**
     * The artifactId of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "artifactId" )
    private String artifactId;

    /**
     * The version of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "version" )
    private String version;

    /**
     * The classifier of the artifact to download. Ignored if {@link #artifact} is used.
     *
     * @since 2.3
     */
    @Parameter( property = "classifier" )
    private String classifier;

    /**
     * The packaging of the artifact to download. Ignored if {@link #artifact} is used.
     */
    @Parameter( property = "packaging", defaultValue = "jar" )
    private String packaging = "jar";

    /**
     * A string of the form groupId:artifactId:version[:packaging[:classifier]].
     */
    @Parameter( property = "artifact" )
    private String artifact;

    /**
     * Download transitively, retrieving the specified artifact and all of its dependencies.
     */
    @Parameter( property = "transitive", defaultValue = "true" )
    private boolean transitive = true;

    /**
     * Skip plugin execution completely.
     *
     * @since 2.7
     */
    @Parameter( property = "mdep.skip", defaultValue = "false" )
    private boolean skip;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping plugin execution" );
            return;
        }

        if ( coordinate.getArtifactId() == null && artifact == null )
        {
            throw new MojoFailureException( "You must specify an artifact, "
                + "e.g. -Dartifact=org.apache.maven.plugins:maven-downloader-plugin:1.0" );
        }
        if ( artifact != null )
        {
            String[] tokens = StringUtils.split( artifact, ":" );
            if ( tokens.length < 3 || tokens.length > 5 )
            {
                throw new MojoFailureException(
                    "Invalid artifact, you must specify groupId:artifactId:version[:packaging[:classifier]] "
                        + artifact );
            }
            coordinate.setGroupId( tokens[0] );
            coordinate.setArtifactId( tokens[1] );
            coordinate.setVersion( tokens[2] );
            if ( tokens.length >= 4 )
            {
                coordinate.setType( tokens[3] );
            }
            if ( tokens.length == 5 )
            {
                coordinate.setClassifier( tokens[4] );
            }
        }

        try
        {
            ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );

            buildingRequest.setRemoteRepositories( repositoryResolver.resolveRepositories() );

            if ( transitive )
            {
                getLog().info( "Resolving " + coordinate + " with transitive dependencies" );
                dependencyResolver.resolveDependencies( buildingRequest, coordinate, null );
            }
            else
            {
                getLog().info( "Resolving " + coordinate );
                artifactResolver.resolveArtifact( buildingRequest, toArtifactCoordinate( coordinate ) );
            }
        }
        catch ( ArtifactResolverException e )
        {
            throw new MojoExecutionException( "Couldn't download artifact: " + e.getMessage(), e );
        }
        catch ( DependencyResolverException e )
        {
            throw new MojoExecutionException( "Couldn't download artifact: " + e.getMessage(), e );
        }
    }

    private ArtifactCoordinate toArtifactCoordinate( DependableCoordinate dependableCoordinate )
    {
        ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( dependableCoordinate.getType() );
        DefaultArtifactCoordinate artifactCoordinate = new DefaultArtifactCoordinate();
        artifactCoordinate.setGroupId( dependableCoordinate.getGroupId() );
        artifactCoordinate.setArtifactId( dependableCoordinate.getArtifactId() );
        artifactCoordinate.setVersion( dependableCoordinate.getVersion() );
        artifactCoordinate.setClassifier( dependableCoordinate.getClassifier() );
        artifactCoordinate.setExtension( artifactHandler.getExtension() );
        return artifactCoordinate;
    }

    protected boolean isSkip()
    {
        return skip;
    }

    // @Parameter( alias = "groupId" )
    public void setGroupId( String groupId )
    {
      this.coordinate.setGroupId( groupId );
    }
    
    // @Parameter( alias = "artifactId" )
    public void setArtifactId( String artifactId )
    {
      this.coordinate.setArtifactId( artifactId );
    }
    
    // @Parameter( alias = "version" )
    public void setVersion( String version )
    {
      this.coordinate.setVersion( version );
    }
    
    // @Parameter( alias = "classifier" )
    public void setClassifier( String classifier )
    {
      this.coordinate.setClassifier( classifier );
    }
    
    // @Parameter( alias = "packaging" )
    public void setPackaging( String type )
    {
      this.coordinate.setType( type );
    }

}
