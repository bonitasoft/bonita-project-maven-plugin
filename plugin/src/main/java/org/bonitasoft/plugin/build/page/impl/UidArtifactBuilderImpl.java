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
package org.bonitasoft.plugin.build.page.impl;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.bonitasoft.plugin.build.page.BuildPageException;
import org.bonitasoft.plugin.build.page.UidArtifactBuilder;
import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.ArtifactBuilderFactory;
import org.bonitasoft.web.designer.JsonHandlerFactory;
import org.bonitasoft.web.designer.config.UiDesignerProperties;
import org.bonitasoft.web.designer.controller.export.ExportException;
import org.bonitasoft.web.designer.model.JsonHandler;
import org.bonitasoft.web.designer.model.JsonViewPersistence;
import org.bonitasoft.web.designer.model.ModelException;
import org.bonitasoft.web.designer.model.page.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UidArtifactBuilderImpl implements UidArtifactBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UidArtifactBuilderImpl.class);

    private JsonHandler jsonHandler = new JsonHandlerFactory().create();
    private ArtifactBuilder artifactBuilder;
    private Path outputDirectory;
    private UiDesignerProperties uiDesignerProperties;

    public UidArtifactBuilderImpl(UiDesignerProperties uiDesignerProperties,
            Path outputDirectory) {
        this.uiDesignerProperties = requireNonNull(uiDesignerProperties);
        this.outputDirectory = requireNonNull(outputDirectory);
    }

    @Override
    public void buildPages(String[] includedPages) throws BuildPageException {
        var pageFolder = uiDesignerProperties.getWorkspace().getPages().getDir();
        var visitor = new PageVisitorImpl(pageFolder, includedPages);
        try {
            Files.walkFileTree(pageFolder, visitor);
            if (visitor.getError() != null) {
                throw new BuildPageException(visitor.getError());
            }
        } catch (IOException e) {
            throw new BuildPageException(e);
        }
    }

    @Override
    public byte[] buildPage(String id) throws BuildPageException {
        if (id == null) {
            throw new IllegalArgumentException("Page id is null");
        }
        try {
            LOGGER.info("Building page {}...", id);
            return getArtifactBuilder().buildPage(id);
        } catch (IOException | ExportException | ModelException e) {
            throw new BuildPageException(e);
        }
    }

    ArtifactBuilder getArtifactBuilder() {
        if (artifactBuilder == null) {
            artifactBuilder = new ArtifactBuilderFactory(uiDesignerProperties).create();
        }
        return artifactBuilder;
    }

    class PageVisitorImpl implements FileVisitor<Path> {

        private Path pageFolder;
        private Throwable error;
        private String[] includedPages;

        public PageVisitorImpl(Path pageFolder, String[] includedPages) {
            this.pageFolder = pageFolder;
            this.includedPages = includedPages;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public FileVisitResult preVisitDirectory(
                Path dir, BasicFileAttributes attrs) {
            if (pageFolder.equals(dir)) {
                return FileVisitResult.CONTINUE;
            }
            if (!Arrays.asList(includedPages).contains(dir.getFileName().toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (!dir.getParent().equals(pageFolder)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(
                Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().endsWith(".json")) {
                try {
                    var page = jsonHandler.fromJson(file, Page.class, JsonViewPersistence.class);
                    var type = page.getType();
                    if ("page".equals(type) || "layout".equals(type)) {
                        LOGGER.info("Building {} {}...", type, page.getName());
                        var content = getArtifactBuilder().build(page);
                        var fileName = String.format("%s_%s.zip", page.getType(), page.getName());
                        LOGGER.info("Writing {} in {}", fileName, outputDirectory);
                        Files.write(outputDirectory.resolve(fileName), content, StandardOpenOption.CREATE);
                    }
                } catch (IOException | ExportException | ModelException e) {
                    error = e;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.SKIP_SIBLINGS;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(
                Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(
                Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }

}
