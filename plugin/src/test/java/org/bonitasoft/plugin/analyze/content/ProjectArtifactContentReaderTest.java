/** 
 * Copyright (C) 2025 BonitaSoft S.A.
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
package org.bonitasoft.plugin.analyze.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader.Entry;
import org.bonitasoft.plugin.analyze.content.ProjectArtifactContentReader.EntryAndCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectArtifactContentReaderTest {

    ProjectArtifactContentReader projArtifactContentReader;
    Artifact artifact;

    @BeforeEach
    void setUp() throws Exception {
        projArtifactContentReader = new ProjectArtifactContentReader(mock(), mock());

        artifact = mock(Artifact.class);
        File projDir = getResourceAsFile("/validation/application_zips_unzipped/application");
        when(artifact.getFile()).thenReturn(projDir);
    }

    private MavenProject mockMavenProject() throws Exception {
        var project = mock(MavenProject.class);
        var mavenBuild = mock(Build.class);
        File target = new File(getResourceAsFile("/validation/application_zips_unzipped/application"), "target");
        when(mavenBuild.getDirectory()).thenReturn(target.toString());
        when(project.getBuild()).thenReturn(mavenBuild);
        return project;
    }

    @Test
    void should_apply_to_folder_and_only_to_folder() throws Exception {
        // given setUp, then
        assertThat(projArtifactContentReader.appliesTo(artifact)).isTrue();

        // given
        var jarArtifact = mock(Artifact.class);
        when(jarArtifact.getFile()).thenReturn(getResourceAsFile("/bonita-actorfilter-single-user-1.0.0.jar"));
        // then
        assertThat(projArtifactContentReader.appliesTo(jarArtifact)).isFalse();

        // given
        var zipArtifact = mock(Artifact.class);
        when(zipArtifact.getFile()).thenReturn(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));
        // then
        assertThat(projArtifactContentReader.appliesTo(zipArtifact)).isFalse();
    }

    @Test
    void should_read_entry() throws Exception {
        // given setUp and
        var readerSpy = spy(projArtifactContentReader);
        doAnswer(invoc -> {
            // make a copy not to delete the original
            var path = (Path) invoc.getArguments()[1];
            var copyPath = Files.createTempFile("tmp", path.getFileName().toString());
            Files.copy(path, copyPath, StandardCopyOption.REPLACE_EXISTING);
            return copyPath;
        }).when(readerSpy).filterDescriptor(any(), any());
        doReturn(mockMavenProject()).when(readerSpy).findMavenProject(any());

        // when
        readerSpy.readEntry(artifact, Path.of("applications", "bonita-user-application.xml"), is -> {
            // then
            try {
                assertThat(is).isNotNull();
                assertThat(new String(is.readAllBytes()))
                        .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // when
        var res = readerSpy.readFirstEntry(artifact, Path.of("applications", "bonita-user-application.xml")::equals,
                entry -> {
                    // then
                    // target compiled path without applications folder
                    assertThat(entry.path()).isEqualTo(Path.of("bonita-user-application.xml"));
                    try (var is = entry.supplier().get()) {
                        assertThat(is).isNotNull();
                        assertThat(new String(is.readAllBytes()))
                                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                        return 1;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        assertThat(res).contains(1);

        // when
        var directTest = readerSpy.hasEntryWithPath(artifact, Path.of("applications", "bonita-user-application.xml"));
        // then
        assertThat(directTest).isTrue();
    }

    @Test
    void should_read_all_entries() throws Exception {
        // given setUp and
        var readerSpy = spy(projArtifactContentReader);
        doReturn(mockMavenProject()).when(readerSpy).findMavenProject(any());
        List<Path> targetPathsFound = new ArrayList<>();
        // when
        readerSpy.readEntries(artifact, path -> true, entry -> {
            targetPathsFound.add(entry.path());
        });
        // then (without applications folder)
        assertThat(targetPathsFound).contains(Path.of("bonita-user-application.xml"),
                Path.of("bonita-user-application.png"));
    }

    @Test
    void should_collect_on_no_entry() throws Exception {
        // given setUp and
        var readerSpy = spy(projArtifactContentReader);
        doReturn(mockMavenProject()).when(readerSpy).findMavenProject(any());
        // when
        var result = readerSpy.readEntries(artifact, Path.of("not_a_file")::equals,
                Collectors.counting());
        // then
        assertThat(result).isZero();
    }

    @Test
    void should_throw_exception_when_read_absent_entry() {
        // given setUp,
        var path = Path.of("test", "not_a_file.txt");
        // then
        assertThrows(IllegalArgumentException.class,
                () -> projArtifactContentReader.readEntry(artifact, path, is -> {
                    throw new RuntimeException("Should not be called");
                }));
    }

    // #detectImplementationHierarchy can not be tested easily without a real maven project. Tested in IT only.

    @Test
    void should_read_clean_filteredDescriptor() throws Exception {
        var readerSpy = spy(projArtifactContentReader);
        AtomicInteger countClean = new AtomicInteger(0);
        // given setUp and
        doAnswer(invoc -> {
            var baseDir = (File) invoc.getArguments()[0];
            var sourcePath = (Path) invoc.getArguments()[1];
            Path relPath = baseDir.toPath().relativize(sourcePath);
            Entry entry = new Entry(relPath, () -> {
                try {
                    return Files.newInputStream(sourcePath);
                } catch (IOException e) {
                    fail(e);
                    return null;
                }
            });
            return new EntryAndCleaner(entry, countClean::incrementAndGet);
        }).when(readerSpy).makeEntry(any(), any());
        doReturn(mockMavenProject()).when(readerSpy).findMavenProject(any());

        // when
        countClean.set(0);
        Optional<Integer> result2 = readerSpy.readFirstEntry(artifact,
                Path.of("applications", "bonita-user-application.xml")::equals,
                entry -> {
                    // return result after opening input stream
                    try (var is = entry.supplier().get()) {
                        return 2;
                    } catch (IOException e) {
                        fail(e);
                        return 0;
                    }
                });

        // then clean was necessarily called
        assertThat(result2).contains(2);
        assertThat(countClean.get()).isEqualTo(1);

        // given filter for 2 files: xml and png
        Predicate<Path> filterTwoFiles = path -> path.getFileName().toString().contains("bonita-user-application");
        // when
        countClean.set(0);
        Collector<Entry, List<Integer>, List<Integer>> collector = Collector.of(
                ArrayList<Integer>::new,
                (l, entry) -> {
                    // add 4 to list after opening input stream
                    try (var is = entry.supplier().get()) {
                        l.add(4);
                    } catch (IOException e) {
                        fail(e);
                    }
                }, (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
        List<Integer> result4 = readerSpy.readEntries(artifact,
                // filter 2 files: xml and png
                filterTwoFiles,
                collector);

        // then clean was necessarily called as many times as needed
        assertThat(result4).containsExactly(4, 4);
        assertThat(countClean.get()).isEqualTo(2);
    }
}
