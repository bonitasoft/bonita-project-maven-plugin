package org.bonitasoft.plugin.analyze;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.CustomPage.CustomPageType;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;

@Named
@Singleton
public class DefaultArtifactAnalyser implements ArtifactAnalyser {

    private final ConnectorResolver connectorResolver;
    private IssueCollector issueCollector;

    @Inject
    public DefaultArtifactAnalyser(ConnectorResolver connectorResolver, IssueCollector issueCollector) {
        this.connectorResolver = connectorResolver;
        this.issueCollector = issueCollector;
    }

    @Override
    public DependencyReport analyse(List<Artifact> artifacts) {
        DependencyReport dependencyReport = new DependencyReport();
        artifacts.forEach(artifact -> {
            try {
                analyze(artifact, dependencyReport);
            } catch (IOException e) {
                throw new AnalysisResultReportException("Failed to analyse artifacts: " + artifact.getId(), e);
            }
        });
        issueCollector.getIssues().forEach(dependencyReport::addIssue);
        return dependencyReport;
    }

    private DependencyReport analyze(Artifact artifact, DependencyReport result) throws IOException {
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

    private void analyseConnectorArtifact(Artifact artifact, DependencyReport result) throws IOException {
        List<Implementation> allImplementations = connectorResolver.findAllImplementations(artifact, issueCollector);
        List<Definition> allDefinitions = connectorResolver.findAllDefinitions(artifact, issueCollector);
        List<ConnectorImplementation> connectorImplementations = allImplementations.stream()
                .filter(ConnectorImplementation.class::isInstance)
                .map(ConnectorImplementation.class::cast)
                .collect(toList());
        List<ActorFilterImplementation> filterImplementations = allImplementations.stream()
                .filter(ActorFilterImplementation.class::isInstance)
                .map(ActorFilterImplementation.class::cast)
                .collect(toList());
        allDefinitions.stream()
                .filter(def -> hasMatchingImplementation(def, connectorImplementations))
                .forEach(result::addConnectorDefinition);
        allDefinitions.stream()
                .filter(def -> hasMatchingImplementation(def, filterImplementations))
                .forEach(result::addFilterDefinition);
        allDefinitions.stream()
                .filter(def -> !hasMatchingImplementation(def,
                        Stream.of(connectorImplementations, filterImplementations)
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())))
                .map(def -> Issue.create(Type.UNKNOWN_DEFINITION_TYPE, String.format("%s declares a definition '%s (%s)' but no matching implementation has been found. This definition will be ignored." ,def.getJarEntry(), def.getDefinitionId(), def.getDefinitionVersion()), Severity.WARNING, artifact.getId()))
                .forEach(result::addIssue);
        connectorImplementations.forEach(result::addConnectorImplementation);
        filterImplementations.forEach(result::addFilterImplementation);
    }

    protected boolean hasMatchingImplementation(Definition def,
            List<? extends Implementation> connectorImplementations) {
        return connectorImplementations.stream()
                .anyMatch(implementation -> Objects.equals(def.getDefinitionId(), implementation.getDefinitionId()) &&
                        Objects.equals(def.getDefinitionVersion(), implementation.getDefinitionVersion()));
    }

    protected void analyseCustomPageArtifact(Artifact artifact, DependencyReport result) throws IOException {
        Properties properties = readPageProperties(artifact.getFile());
        String contentType = properties.getProperty("contentType");
        CustomPageType customPageType = CustomPageType.valueOf(contentType.toUpperCase());
        String name = properties.getProperty(CustomPage.NAME_PROPERTY);
        String displayName = properties.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
        String description = properties.getProperty(CustomPage.DESCRIPTION_PROPERTY);
        switch (customPageType) {
            case FORM:
                result.addForm(Form.create(name, displayName, description, create(artifact)));
                break;
            case PAGE:
                result.addPage(Page.create(name, displayName, description, create(artifact)));
                break;
            case THEME:
                result.addTheme(Theme.create(name, displayName, description, create(artifact)));
                break;
            case APIEXTENSION:
                result.addRestAPIExtension(RestAPIExtension.create(name, displayName, description, create(artifact)));
                break;
            default:
                throw new AnalysisResultReportException("Unsupported Custom Page type: " + contentType);
        }
    }

    private static org.bonitasoft.plugin.analyze.report.model.Artifact create(Artifact artifact) {
        return org.bonitasoft.plugin.analyze.report.model.Artifact.create(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getBaseVersion() == null ? artifact.getVersion() : artifact.getBaseVersion(),
                artifact.getClassifier(),
                artifact.getFile().getAbsolutePath());
    }

    protected boolean hasConnectorDescriptor(File artifactFile) throws IOException {
        return findJarEntry(artifactFile, entry -> entry.getName().endsWith(".impl"))
                .isPresent();
    }

    protected boolean hasCustomPageDescriptor(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().equals("page.properties"))
                .isPresent();
    }

    private static Optional<? extends ZipEntry> findZipEntry(File file, Predicate<? super ZipEntry> entryPredicate)
            throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            return zipFile.stream()
                    .filter(entryPredicate)
                    .findFirst();
        }
    }

    protected Properties readPageProperties(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().equals("page.properties"))
                .map(entry -> {
                    try (ZipFile zipFile = new ZipFile(artifactFile);
                            Reader reader = new InputStreamReader(zipFile.getInputStream(entry),
                                    StandardCharsets.UTF_8)) {
                        Properties prop = new Properties();
                        prop.load(reader);
                        return prop;
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .orElseThrow(
                        () -> new IllegalArgumentException(format("No page.properties found in %s", artifactFile)));
    }

    static Optional<JarEntry> findJarEntry(File file, Predicate<? super JarEntry> entryPredicate)
            throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.stream()
                    .filter(entryPredicate)
                    .findFirst();
        }
    }

}
