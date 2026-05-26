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

package org.jdrupes.builder.java;

import com.google.common.flogger.FluentLogger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.Version;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProviderSpi;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.api.ResourceType.*;
import org.jdrupes.builder.api.Resources;
import org.jdrupes.builder.api.TarFile;
import org.jdrupes.builder.api.ZipFile;
import org.jdrupes.builder.core.AbstractGenerator;
import org.jdrupes.builder.core.StreamCollector;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.internal.ApplicationConfigurationData;
import org.jdrupes.builder.java.internal.TarDistributionBuilder;
import org.jdrupes.builder.java.internal.ZipDistributionBuilder;

/// The [ApplicationBuilder] generates resources of type
/// [ApplicationZipFile] or [ApplicationTarFile].
///
/// Both resource types are runnable application distributions built from
/// a set of classpath resources and a generated start script that launches
/// the application.
///
/// The application can be configured using methods that control:
/// 
///   * the [output directory][#destination(Path)] for the generated
///     distribution,
///   * the [base name][#distributionBaseName(Supplier)] of the generated
///     archive file,
///   * the executable (start script) [name][#executableName(String)],
///   * the [main class][#mainClassName(String)] to execute (mandatory),
///   * and the [JVM options][#applicationJvmOpts(Consumer)] required
///     by the application and included in the generated start script.
///
/// Method [#add(Stream)] is used to specify the classpath resources to
/// be included in the generated distribution and to be added to the
/// classpath when running the application.
/// 
/// A request for [Cleanliness] removes any generated distribution
/// archives from the configured destination directory.
///
public class ApplicationBuilder extends AbstractGenerator {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private Supplier<Path> destination
        = () -> project().buildDirectory().resolve("distributions");
    private Supplier<String> distributionBaseName
        = () -> project().name() + "-" + project().get(Version);
    private final StreamCollector<ClasspathElement> resourceStreams
        = StreamCollector.cached();
    private final ApplicationConfigurationData config
        = new ApplicationConfigurationData();

    /// Initializes a new application builder.
    ///
    /// @param project the project
    ///
    public ApplicationBuilder(Project project) {
        super(Objects.requireNonNull(project));
        config.executableName(project().name());
    }

    @Override
    public ApplicationBuilder name(String name) {
        rename(name);
        return this;
    }

    /// Returns the name of the script that starts the application.
    /// The script for Windows has `.bat` appended to this name.
    ///
    /// @return the string
    ///
    public String executableName() {
        return config.executableName();
    }

    /// Sets the executable name.
    ///
    /// @param name the name
    /// @return the application builder
    ///
    public ApplicationBuilder executableName(String name) {
        config.executableName(name);
        return this;
    }

    /// Returns the destination directory. Defaults to sub directory
    /// `applications` in the project's build directory
    /// (see [Project#buildDirectory]).
    ///
    /// @return the destination
    ///
    public Path destination() {
        return destination.get();
    }

    /// Sets the destination directory. The [Path] is resolved against
    /// the project's build directory (see [Project#buildDirectory]).
    ///
    /// @param destination the new destination
    /// @return the application builder
    ///
    public ApplicationBuilder destination(Path destination) {
        this.destination
            = () -> project().buildDirectory().resolve(destination);
        return this;
    }

    /// Sets the destination directory.
    ///
    /// @param destination the new destination
    /// @return the jar generator
    ///
    public ApplicationBuilder destination(Supplier<Path> destination) {
        this.destination = destination;
        return this;
    }

    /// Returns the base name of the generated TAR or ZIP file. The base
    /// name is the file name without the extension. Defaults to the 
    /// project's name followed by its version.
    ///
    /// @return the string
    ///
    public String distributionBaseName() {
        return distributionBaseName.get();
    }

    /// Sets the supplier for obtaining the name of the generated
    /// ZIP or TAR file's base name in [ResourceProviderSpi#provide].
    ///
    /// @param distributionBaseName the distribution base name
    /// @return the application builder
    ///
    public ApplicationBuilder
            distributionBaseName(Supplier<String> distributionBaseName) {
        this.distributionBaseName = distributionBaseName;
        return this;
    }

    /// Returns the main class name.
    ///
    /// @return the main class name
    ///
    public String mainClassName() {
        return config.mainClassName();
    }

    /// Sets the name of the main class (the application entry point).
    ///
    /// @param name the new main class name
    /// @return the jar generator for method chaining
    ///
    public ApplicationBuilder mainClassName(String name) {
        config.mainClassName(Objects.requireNonNull(name));
        return this;
    }

    /// Passes the mutable list of JVM options to the given consumer for
    /// modification. The start script distinguishes between these options,
    /// which reflect settings required by the application, and the
    /// `JAVA_OPTS` that may be used when starting the application to tune
    /// the JVM for specific environments.
    ///
    /// @param modifier the modifier
    /// @return the list
    ///
    public ApplicationBuilder
            applicationJvmOpts(Consumer<List<String>> modifier) {
        modifier.accept(config.applicationJvmOpts());
        return this;
    }

    /// Adds the given classpath resources to the application.
    ///
    /// @param resources the resources
    /// @return the application builder
    ///
    public ApplicationBuilder
            add(Stream<? extends ClasspathElement> resources) {
        resourceStreams.add(resources);
        return this;
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> request) {
        if (!request.accepts(ApplicationZipFileType)
            && !request.accepts(ApplicationTarFileType)
            && !request.accepts(CleanlinessType)) {
            return Collections.emptyList();
        }

        // Maybe only delete
        if (request.accepts(CleanlinessType)) {
            destination()
                .resolve(distributionBaseName() + ".zip").toFile().delete();
            destination()
                .resolve(distributionBaseName() + ".tar").toFile().delete();
            return Collections.emptyList();
        }

        // Make sure mainClass is set
        if (mainClassName() == null) {
            throw new ConfigurationException().from(this)
                .message("Main class must be set for %s", name());
        }

        // Prepare the application file
        var destDir = destination();
        if (!destDir.toFile().exists() && !destDir.toFile().mkdirs()) {
            throw new ConfigurationException().from(this)
                .message("Cannot create directory " + destDir);
        }
        Resources<ClasspathElement> cpes
            = Resources.of(new ResourceType<>() {});
        cpes.addAll(resourceStreams.stream());
        FileResource distFile;
        if (request.accepts(ApplicationZipFileType)) {
            distFile = buildZip(cpes);
        } else {
            distFile = buildTar(cpes);
        }
        @SuppressWarnings("unchecked")
        var result = (T) distFile;
        return List.of(result);
    }

    private FileResource buildZip(Resources<ClasspathElement> cpes) {
        var zipFile = ZipFile.of(ApplicationZipFileType,
            destination().resolve(distributionBaseName() + ".zip"));
        if (cpes.isNewerThan(zipFile)) {
            logger.atInfo().log("%s building %s", this, zipFile);
            new ZipDistributionBuilder().build(zipFile, config, cpes);
        } else {
            logger.atFine().log("%s found %s to be up to date", this, zipFile);
        }
        return zipFile;
    }

    private FileResource buildTar(Resources<ClasspathElement> cpes) {
        var tarFile = TarFile.of(ApplicationTarFileType,
            destination().resolve(distributionBaseName() + ".tar"));
        if (cpes.isNewerThan(tarFile)) {
            logger.atInfo().log("%s building %s", this, tarFile);
            new TarDistributionBuilder().build(tarFile, config, cpes);
        } else {
            logger.atFine().log("%s found %s to be up to date", this, tarFile);
        }
        return tarFile;
    }
}
