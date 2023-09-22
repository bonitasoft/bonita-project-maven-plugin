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
package org.bonitasoft.plugin.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.bonitasoft.plugin.test.TestFiles;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CopyProvidedPagesMojoTest {

    CopyProvidedPagesMojo mojo;
    @Mock
    ArtifactResolver artifactResolver;
    @Mock
    MavenProject project;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MavenSession session;
    @Mock
    LocalRepositoryManager localRepositoryManager;

    @BeforeEach
    void setUp() {
        mojo = spy(new CopyProvidedPagesMojo(artifactResolver));
        mojo.project = project;
        mojo.session = session;
        when(session.getRepositorySession().getLocalRepositoryManager()).thenReturn(localRepositoryManager);
    }

    @Test
    void should_detect_provided_pages() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages/with-provided-pages");
        mockProjectDependencies();

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        List<String> expectedPages = CopyProvidedPagesMojo.PROVIDED_PAGES.values().stream()
                .map(DefaultArtifactCoordinate::getArtifactId).collect(Collectors.toList());
        assertThat(providedPages).hasSize(expectedPages.size());
        providedPages.forEach(page -> assertThat(page.getArtifactId()).isIn(expectedPages));
    }

    @Test
    void should_not_detect_provided_pages() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages/without-provided-pages");

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        assertThat(providedPages).isEmpty();
        verify(mojo).toApplicationContainerNode(any());
        verify(mojo).listPages(any());
        verify(mojo, never()).setArtifactVersion(any());
    }

    @Test
    void should_detect_provided_pages_with_multiple_applications() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages/multiple-applications");
        mockProjectDependencies();

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        List<String> expectedPages = CopyProvidedPagesMojo.PROVIDED_PAGES.values().stream()
                .map(DefaultArtifactCoordinate::getArtifactId).collect(Collectors.toList());
        assertThat(providedPages).hasSize(expectedPages.size());
        providedPages.forEach(page -> assertThat(page.getArtifactId()).isIn(expectedPages));
        verify(mojo, times(3)).toApplicationContainerNode(any());
        verify(mojo, times(3)).listPages(any());
        verify(mojo, times(4)).setArtifactVersion(any());
    }

    @Test
    void should_not_detect_provided_pages_with_empty_application() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages/empty-application");

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        assertThat(providedPages).isEmpty();
        verify(mojo).toApplicationContainerNode(any());
        verify(mojo).listPages(any());
        verify(mojo, never()).setArtifactVersion(any());
    }

    @Test
    void should_not_detect_provided_pages_with_invalid_application() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages/invalid-application");

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        assertThat(providedPages).isEmpty();
        verify(mojo).toApplicationContainerNode(any());
        verify(mojo, never()).listPages(any());
        verify(mojo, never()).setArtifactVersion(any());
    }

    @Test
    void should_not_detect_provided_pages_with_not_application_xml() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages/not-application-xml");

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        assertThat(providedPages).isEmpty();
        verify(mojo, never()).toApplicationContainerNode(any());
        verify(mojo, never()).listPages(any());
        verify(mojo, never()).setArtifactVersion(any());
    }

    @Test
    void should_ignore_detection_if_no_application_directory() throws Exception {
        //given
        mockProjectBasedir("/build/provided-pages");

        //when
        var providedPages = mojo.detectProvidedPages();

        //then
        assertThat(providedPages).isEmpty();
        verify(mojo, never()).toApplicationContainerNode(any());
        verify(mojo, never()).listPages(any());
        verify(mojo, never()).setArtifactVersion(any());
    }

    private void mockProjectBasedir(String basedir) throws URISyntaxException {
        when(project.getBasedir()).thenReturn(TestFiles.getResourceAsFile(basedir));
    }

    private void mockProjectDependencies() {
        when(project.getDependencyManagement()).thenReturn(buildDependencyManagement());
    }

    private DependencyManagement buildDependencyManagement() {
        var dependencyManagement = new DependencyManagement();
        dependencyManagement.setDependencies(List.of(
                buildDependency("foo.bar", "foo-bar", "f.o.o-bar"),
                buildDependency("org.bonitasoft.web.page", "page-user-task-list", "1.0.0"),
                buildDependency("org.bonitasoft.web.page", "page-user-process-list", "1.0.0"),
                buildDependency("org.bonitasoft.web.page", "page-user-case-list", "1.0.0"),
                buildDependency("org.bonitasoft.web.page", "page-user-case-details", "1.0.0")));
        return dependencyManagement;
    }

    private Dependency buildDependency(String groupId, String artifactId, String versionId) {
        var dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(versionId);
        return dependency;
    }
}
