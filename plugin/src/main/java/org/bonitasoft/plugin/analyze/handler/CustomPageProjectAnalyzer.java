/** 
 * Copyright (C) 2023 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.eclipse.aether.repository.LocalRepositoryManager;

class CustomPageProjectAnalyzer extends CustomPageAnalyzer {

    private static final String CUSTOMPAGE_DESCRIPTOR_PROPERTIES = "page.properties";

    private MavenResourcesFiltering mavenResourcesFiltering;
    private List<MavenProject> reactorProjects;

    CustomPageProjectAnalyzer(LocalRepositoryManager localRepositoryManager,
            MavenResourcesFiltering mavenResourcesFiltering, List<MavenProject> reactorProjects) {
        super(localRepositoryManager);
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.reactorProjects = reactorProjects;
    }

    @Override
    public boolean appliesTo(Artifact artifact) {
        var basedir = artifact.getFile();
        var pageDescriptor = findCustomPageDescriptor(basedir);
        return Files.exists(pageDescriptor);
    }

    private Path findCustomPageDescriptor(File basedir) {
        var pageDescriptor = basedir.toPath().resolve(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
        if (!Files.exists(pageDescriptor)) {
            pageDescriptor = basedir.toPath().resolve("src/main/resources")
                    .resolve(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
        }
        return pageDescriptor;
    }

    @Override
    public DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException {
        var filteredPageDescriptor = filterPageDescriptor(artifact.getFile(),
                findCustomPageDescriptor(artifact.getFile()));
        var properties = readPageProperties(filteredPageDescriptor);
        analyzeCustomPageArtifact(artifact, properties, report);
        return report;
    }

    private Path filterPageDescriptor(File basedir, Path pageDescriptor) throws IOException {
        var mavenResource = newFilteredResource(pageDescriptor);
        var mavenResourcesExecution = newMavenResourcesExecution(mavenResource, basedir);
        try {
            mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException e) {
            throw new IOException(e);
        }
        return mavenResourcesExecution.getOutputDirectory().toPath().resolve(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
    }

    private MavenResourcesExecution newMavenResourcesExecution(Resource resource, File basedir) {
        var mavenResourcesExecution = new MavenResourcesExecution();
        mavenResourcesExecution.setResources(List.of(resource));
        var mavenProject = reactorProjects.stream()
                .filter(p -> Objects.equals(basedir, p.getBasedir()))
                .findFirst().orElseThrow();
        mavenResourcesExecution.setMavenProject(mavenProject);
        var outputFolder = new File(mavenProject.getBuild().getDirectory());
        mavenResourcesExecution.setOutputDirectory(outputFolder);
        mavenResourcesExecution.setUseDefaultFilterWrappers(true);
        mavenResourcesExecution.setFilterWrappers(List.of());
        mavenResourcesExecution.setEncoding("UTF-8");
        mavenResourcesExecution.setPropertiesEncoding("UTF-8");
        return mavenResourcesExecution;
    }

    private Resource newFilteredResource(Path pageDescriptor) {
        var mavenResource = new Resource();
        mavenResource.setDirectory(pageDescriptor.getParent().toString());
        mavenResource.setIncludes(List.of(pageDescriptor.getFileName().toString()));
        mavenResource.setFiltering(true);
        return mavenResource;
    }

    Properties readPageProperties(Path pageDescriptor) throws IOException {
        try (var in = new InputStreamReader(
                new FileInputStream(pageDescriptor.toFile()), StandardCharsets.UTF_8)) {
            Properties prop = new Properties();
            prop.load(in);
            return prop;
        } finally {
            Files.delete(pageDescriptor);
        }
    }

}
