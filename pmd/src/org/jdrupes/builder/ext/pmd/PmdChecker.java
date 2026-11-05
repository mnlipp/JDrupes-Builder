/*
 * JDrupes Builder
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.builder.ext.pmd;

import com.google.common.flogger.FluentLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.rule.RuleSetLoadException;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.Report.ProcessingError;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.ext.pmd.PmdTypes.*;
import org.jdrupes.builder.java.JavaSourceFile;

/// A [Generator] that runs PMD on the project's Java sources.
///
/// The PMD ruleset configuration is loaded from a file named
/// `pmd.xml`. First, the project's directory is searched. If not
/// found, the search continues in the parent project's directory and
/// so on, up to the root project. The configuration path can also be
/// set explicitly with [config].
///
/// Each rule violation is printed to [context().error()]. The
/// resulting [PmdResult] contains summary counts and is never flagged
/// as faulty.
/// 
/// This provider is made available as an extension.
/// [![org.jdrupes:jdbld-ext-checkstyle:](
/// https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcodeberg.org%2Fapi%2Fpackages%2FJDrupes%2Fmaven%2Forg%2Fjdrupes%2Fjdbld-ext-checkstyle%2Fmaven-metadata.xml&strategy=releaseProperty)
/// ](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes:jdbld-ext-checkstyle/versions)
///
public class PmdChecker extends AbstractGenerator {

    private static final FluentLogger logger
        = FluentLogger.forEnclosingClass();

    private final Resources<FileTree<JavaSourceFile>> sources
        = Resources.of(new ResourceType<>() {});
    private Path configPath;

    /// Initializes a new PMD checker.
    ///
    /// @param project the project
    ///
    public PmdChecker(Project project) {
        super(project);
    }

    /// Sets the path to the PMD ruleset configuration file. The [Path]
    /// is resolved against the project's directory when left relative.
    ///
    /// @param configPath the configuration file path
    /// @return the PMD checker
    ///
    public PmdChecker config(Path configPath) {
        this.configPath = configPath;
        return this;
    }

    /// Adds the source tree.
    ///
    /// @param sources the sources
    /// @return the PMD checker
    ///
    public final PmdChecker addSources(FileTree<JavaSourceFile> sources) {
        this.sources.add(sources);
        return this;
    }

    /// Adds the files from the given directory matching the given pattern.
    /// Short for
    /// `addSources(FileTree.of(project(), directory, JavaSourceFile.class, pattern))`.
    ///
    /// @param directory the directory
    /// @param pattern the pattern
    /// @return the PMD checker
    ///
    public final PmdChecker addSources(Path directory, String pattern) {
        addSources(FileTree.of(
            project(), directory, JavaSourceFile.class, pattern));
        return this;
    }

    /// Returns the source trees configured for the checker.
    ///
    /// @return the resources
    ///
    public Resources<FileTree<JavaSourceFile>> sources() {
        return sources;
    }

    @Override
    protected <T extends Resource> Collection<T> doProvide(
            ResourceRequest<T> requested) {
        if (!requested.accepts(PmdResultType)) {
            return Collections.emptyList();
        }
        if (!requested.isFor(PmdResultType)) {
            @SuppressWarnings("unchecked")
            var result = (Collection<T>) resources(of(PmdResultType))
                .toList();
            return result;
        }

        Path effectiveConfig = configPath;
        if (effectiveConfig == null) {
            effectiveConfig = findConfig();
        }
        if (effectiveConfig == null) {
            throw new ConfigurationException().from(this).message("""
                No configuration file found.""");
        }

        logger.atInfo().log("Running PMD on %s with ruleset %s",
            project().name(), effectiveConfig);

        List<Path> files = sourceFiles();
        int checkedCount = 0;

        PMDConfiguration config = new PMDConfiguration();
        config.setIgnoreIncrementalAnalysis(true);
        config.addRelativizeRoot(project().directory());

        try (PmdAnalysis analysis = PmdAnalysis.create(config)) {
            try {
                analysis.addRuleSet(
                    analysis.newRuleSetLoader()
                        .loadFromResource(effectiveConfig.toString()));
            } catch (RuleSetLoadException e) {
                throw new ConfigurationException()
                    .from(this).cause(e).message("""
                        Could not load PMD ruleset from %s.""",
                        effectiveConfig);
            }

            for (Path file : files) {
                analysis.files().addFile(file);
                checkedCount++;
            }

            Report report = analysis.performAnalysisAndCollectReport();

            for (ProcessingError error : report.getProcessingErrors()) {
                logger.atFiner().withCause(error.getError())
                    .log("[%s] %s: %s%n", project(), error.getFileId(),
                        error.getMsg());
            }
            for (RuleViolation violation : report.getViolations()) {
                context().error().format("[%s] %s:%d:%d: %s [%s]%n",
                    project(), project().directory().relativize(
                        Path.of(violation.getFileId().getAbsolutePath())),
                    violation.getBeginLine(),
                    violation.getBeginColumn(),
                    violation.getDescription(),
                    violation.getRule().getName());
            }

            var result = PmdResult.of(project(), this, checkedCount,
                (long) report.getViolations().size());
            @SuppressWarnings("unchecked")
            var asList = List.of((T) result);
            return asList;
        }
    }

    private Path findConfig() {
        Project current = project();
        while (current != null) {
            Path candidate = current.directory().resolve("pmd.xml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            if (current.equals(current.rootProject())) {
                return null;
            }
            current = current.parentProject().orElse(null);
        }
        return null;
    }

    private List<Path> sourceFiles() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .toList();
    }
}
