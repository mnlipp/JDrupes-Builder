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

package org.jdrupes.builder.ext.checkstyle;

import com.google.common.flogger.FluentLogger;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader.IgnoredModulesOptions;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
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
import static org.jdrupes.builder.ext.checkstyle.CheckstyleTypes.*;
import org.jdrupes.builder.java.JavaSourceFile;

/// A [Generator] that runs checkstyle on the project's Java sources.
///
/// The checkstyle configuration is loaded from a file named
/// `checkstyle.xml`. First, the project's directory is searched. If
/// not found, the search continues in the parent project's directory
/// and so on, up to the root project. The configuration path can also
/// be set explicitly with [config].
///
/// Each style violation is printed to [context().error()]. The resulting
/// [CheckstyleResult] contains summary counts. When errors are found,
/// the result is flagged as faulty unless [ignoreFailed] was invoked.
/// 
/// This provider is made available as an extension.
/// [![org.jdrupes:jdbld-ext-pmd:](
/// https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcodeberg.org%2Fapi%2Fpackages%2FJDrupes%2Fmaven%2Forg%2Fjdrupes%2Fjdbld-ext-pmd%2Fmaven-metadata.xml&strategy=releaseProperty)
/// ](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes:jdbld-ext-pmd/versions)
///
public class CheckstyleChecker extends AbstractGenerator {

    private static final FluentLogger logger
        = FluentLogger.forEnclosingClass();

    private final Resources<FileTree<JavaSourceFile>> sources
        = Resources.of(new ResourceType<>() {});
    private Path configPath;
    private boolean ignoreFailed;

    /// Initializes a new style checker.
    ///
    /// @param project the project
    ///
    public CheckstyleChecker(Project project) {
        super(project);
    }

    /// Ignore errors. If invoked, the checker does not set the
    /// faulty flag of the checkstyle result when errors have been found.
    ///
    /// @return the checkstyle checker
    ///
    public CheckstyleChecker ignoreFailed() {
        this.ignoreFailed = true;
        return this;
    }

    /// Sets the path to the checkstyle configuration file. The [Path]
    /// is resolved against the project's directory when left relative.
    ///
    /// @param configPath the configuration file path
    /// @return the style checker
    ///
    public CheckstyleChecker config(Path configPath) {
        this.configPath = configPath;
        return this;
    }

    /// Adds the source tree.
    ///
    /// @param sources the sources
    /// @return the style checker
    ///
    public final CheckstyleChecker
            addSources(FileTree<JavaSourceFile> sources) {
        this.sources.add(sources);
        return this;
    }

    /// Adds the files from the given directory matching the given pattern.
    /// Short for
    /// `addSources(FileTree.of(project(), directory, JavaSourceFile.class, pattern))`.
    ///
    /// @param directory the directory
    /// @param pattern the pattern
    /// @return the style checker
    ///
    public final CheckstyleChecker addSources(Path directory, String pattern) {
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

    private Path findConfig() {
        Project current = project();
        while (current != null) {
            Path candidate = current.directory().resolve("checkstyle.xml");
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

    private List<File> sourceFiles() {
        return sources.stream().map(Resources::stream)
            .flatMap(Function.identity()).map(FileResource::path)
            .map(Path::toFile).toList();
    }

    @Override
    protected <T extends Resource> Collection<T> doProvide(
            ResourceRequest<T> requested) {
        if (!requested.accepts(CheckstyleResultType)) {
            return Collections.emptyList();
        }
        if (!requested.isFor(CheckstyleResultType)) {
            @SuppressWarnings("unchecked")
            var result = (Collection<T>) resources(of(CheckstyleResultType))
                .toList();
            return result;
        }

        Path effectiveConfig = configPath;
        if (effectiveConfig == null) {
            effectiveConfig = findConfig();
        }
        if (effectiveConfig == null) {
            throw new ConfigurationException().from(this).message("""
                No checkstyle.xml found in this project, 
                its parent projects, or the root project.
                Set config() explicitly.""");
        }

        logger.atInfo().log("Running checkstyle on %s with config %s",
            project().name(), effectiveConfig);

        var summary = new AuditSummary();
        List<File> files = sourceFiles();

        Checker checker = new Checker();
        try {
            checker.setModuleClassLoader(
                CheckstyleChecker.class.getClassLoader());
            var configuration = ConfigurationLoader.loadConfiguration(
                effectiveConfig.toString(),
                new PropertiesExpander(new Properties()),
                IgnoredModulesOptions.EXECUTE);
            checker.configure(configuration);
            checker.addListener(summary);

            int errors = checker.process(files);
            checker.destroy();
            long checked = summary.checkedCount();
            long warnings = summary.warningsCount();

            var result = CheckstyleResult.of(project(), this, checked,
                errors, warnings);
            if (errors > 0 && !ignoreFailed) {
                result.setFaulty();
            }
            @SuppressWarnings("unchecked")
            var asList = List.of((T) result);
            return asList;
        } catch (CheckstyleException e) {
            throw new ConfigurationException().from(this).cause(e)
                .message("CheckstyleChecker failed: %s", e.getMessage());
        } finally {
            checker.destroy();
        }
    }

    /// Counts checked files and delegates violation output to context.
    ///
    private final class AuditSummary implements AuditListener {

        private int checkedCount;
        private long warningsCount;

        /// Return the number of files checked.
        ///
        /// @return the int
        ///
        /* default */ int checkedCount() {
            return checkedCount;
        }

        /// Return the number of warnings.
        ///
        /// @return the long
        ///
        /* default */ long warningsCount() {
            return warningsCount;
        }

        @Override
        public void auditStarted(AuditEvent event) {
            // Not tracked
        }

        @Override
        public void auditFinished(AuditEvent event) {
            // Not tracked
        }

        @Override
        public void fileStarted(AuditEvent event) {
            checkedCount++;
        }

        @Override
        public void fileFinished(AuditEvent event) {
            // Not tracked
        }

        @Override
        public void addError(AuditEvent event) {
            if (event.getSeverityLevel() == SeverityLevel.WARNING) {
                warningsCount++;
            }
            context().error().format("[%s] %s:%d:%d: %s%n",
                project(),
                project().directory().relativize(Path.of(event.getFileName())),
                event.getLine(), event.getColumn(), event.getMessage());
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable) {
            logger.atWarning().withCause(throwable)
                .log("CheckstyleChecker exception in %s", event.getFileName());
        }
    }
}
