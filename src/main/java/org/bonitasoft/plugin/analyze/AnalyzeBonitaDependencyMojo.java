/**
 * Copyright (C) 2020 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Definition;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Form;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Implementation;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Page;
import org.bonitasoft.plugin.analyze.BonitaArtifact.RestAPIExtension;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Theme;

@Mojo(name = "analyze", defaultPhase = LifecyclePhase.NONE)
public class AnalyzeBonitaDependencyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * Remote repositories which will be searched for artifacts.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Local Repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    protected ArtifactRepository localRepository;
    
    /**
     * Analysis report output file
     */
    @Parameter(defaultValue = "${project.build.directory}/bonita-dependencies.csv", required = true)
    private File outputFile;

    private final ArtifactResolver artifactResolver;
    private final ConnectorResolver connectorResolver;
    
    @Inject
    public AnalyzeBonitaDependencyMojo(ArtifactResolver artifactResolver, ConnectorResolver connectorResolver) {
        this.connectorResolver = connectorResolver;
        this.artifactResolver = artifactResolver;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Artifact> artifacts = project.getDependencyArtifacts();
        AnalysisResult analysisResult = new AnalysisResult();
        for (Artifact artifact : artifacts) {
            ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
            try {
                ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, artifact);
                File artifactFile = result.getArtifact().getFile();
                if (artifactFile == null || !artifactFile.exists()) {
                    throw new MojoExecutionException(String.format("Failed to resolve artifact %s", artifact));
                }
                analyze(result.getArtifact(), analysisResult);
            } catch (Exception e) {
                throw new MojoExecutionException(String.format("Failed to resolve artifact %s", artifact), e);
            }
        }
        analysisResult.printResult(getLog());
        try {
            analysisResult.writeCSVOutput(outputFile);
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Failed to write report file %s", outputFile), e);
        }
    }

    private AnalysisResult analyze(Artifact artifact, AnalysisResult result) throws Exception {
        File artifactFile = artifact.getFile();
        String fileName = artifactFile.getName();
        if (fileName.endsWith(".jar") && hasConnectorDescriptor(artifactFile)) {
            analyseConnectorArtifact(artifact, result);
        }
        if (fileName.endsWith(".zip") && hasCustomPageDescriptor(artifactFile)) {
            analyseCustomPageArtifact(artifact, result);
        }
        return result;
    }

    private void analyseConnectorArtifact(Artifact artifact, AnalysisResult result) throws Exception {
        List<Implementation> allImplementations = connectorResolver.findAllImplementations(artifact);
        List<Definition> allDefinitions = connectorResolver.findAllDefinitions(artifact);
        List<Implementation> connectorImplementations = allImplementations.stream()
                .filter(impl -> ConnectorResolver.ABSTRACT_CONNECTOR_TYPE.equals(impl.getSuperType()))
                .collect(Collectors.toList());
        List<Implementation> filterImplementations = allImplementations.stream()
                .filter(impl -> ConnectorResolver.ABSTRACT_FILTER_TYPE.equals(impl.getSuperType()))
                .collect(Collectors.toList());
        allDefinitions.stream()
                .filter(def -> hasMatchingImplementation(def, connectorImplementations))
                .forEach(result::addConnectorDefinition);
        allDefinitions.stream()
                .filter(def -> hasMatchingImplementation(def, filterImplementations))
                .forEach(result::addFilterDefinition);
        connectorImplementations.stream().forEach(result::addConnectorImplementation);
        filterImplementations.stream().forEach(result::addFilterImplementation);
    }

    private boolean hasMatchingImplementation(Definition def, List<Implementation> connectorImplementations) {
        return connectorImplementations.stream()
                .anyMatch(implementation -> Objects.equals(def.getDefinitionId(), implementation.getDefinitionId()) &&
                        Objects.equals(def.getDefinitionVersion(), implementation.getDefinitionVersion()));
    }
    
    private void analyseCustomPageArtifact(Artifact artifact, AnalysisResult result) throws Exception {
        Properties properties = readPageProperties(artifact.getFile());
        String contentType = properties.getProperty("contentType");
        if ("form".equals(contentType)) {
            result.addForm(Form.create(properties, artifact));
        }
        if ("page".equals(contentType)) {
            result.addPage(Page.create(properties, artifact));
        }
        if ("theme".equals(contentType)) {
            result.addTheme(Theme.create(properties, artifact));
        }
        if ("apiExtension".equals(contentType)) {
            result.addRestAPIExtension(RestAPIExtension.create(properties, artifact));
        }
    }

    private boolean hasConnectorDescriptor(File artifactFile) throws IOException {
        try (JarFile jarFile = new JarFile(artifactFile)) {
            Enumeration<JarEntry> enumOfJar = jarFile.entries();
            while (enumOfJar.hasMoreElements()) {
                JarEntry jarEntry = enumOfJar.nextElement();
                String name = jarEntry.getName();
                if (name.endsWith(".impl")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCustomPageDescriptor(File artifactFile) throws IOException {
        try (ZipFile zipFile = new ZipFile(artifactFile)) {
            Enumeration<? extends ZipEntry> enumOfZip = zipFile.entries();
            while (enumOfZip.hasMoreElements()) {
                ZipEntry zipEntry = enumOfZip.nextElement();
                String name = zipEntry.getName();
                if (name.equals("page.properties")) {
                    return true;
                }
            }
        }
        return false;
    }

    private Properties readPageProperties(File artifactFile) throws IOException {
        try (ZipFile zipFile = new ZipFile(artifactFile)) {
            Enumeration<? extends ZipEntry> enumOfZip = zipFile.entries();
            while (enumOfZip.hasMoreElements()) {
                ZipEntry zipEntry = enumOfZip.nextElement();
                String name = zipEntry.getName();
                if (name.equals("page.properties")) {
                    Properties prop = new Properties();
                    prop.load(zipFile.getInputStream(zipEntry));
                    return prop;
                }
            }
        }
        throw new IllegalArgumentException(String.format("No page.properties found in %s", artifactFile));
    }

    /*
     * @return Returns a new ProjectBuildingRequest populated from the current session and the current project remote
     * repositories, used to resolve artifacts.
     */
    private ProjectBuildingRequest newResolveArtifactProjectBuildingRequest() {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

        buildingRequest.setRemoteRepositories(remoteRepositories);

        return buildingRequest;
    }

}
