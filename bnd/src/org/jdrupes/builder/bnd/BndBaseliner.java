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

package org.jdrupes.builder.bnd;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Diff;
import com.google.common.flogger.FluentLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Generator;
import static org.jdrupes.builder.api.Intent.Supply;
import org.jdrupes.builder.api.Project;
import static org.jdrupes.builder.api.Project.Properties.Version;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.api.Resources;
import static org.jdrupes.builder.bnd.BndTypes.*;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.java.LibraryJarFile;
import static org.jdrupes.builder.mvnrepo.MvnProperties.*;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;

/// A [Generator] that performs a baseline evaluation between two OSGi
/// bundles using the `bndlib` library [bnd](https://github.com/bndtools/bnd).
/// 
/// Because OSGi repositories never became popular, Maven repository
/// semantics are used to find the baseline bundle. The current bundle
/// is the library supplied by the project. The [BndBaseliner] evaluates
/// its Maven coordinates in the same way as the [PomFileGenerator] does.
/// From these, coordinates used to lookup the previous version are derived
/// in the form `groupId:artifactId:[,version)`
/// 
/// The [BndBaseliner] then performs the baseline evaluation. Instructions
/// `-diffignore` and `-diffpackages` are supported and forwarded to
/// `bndlib`.
///
@SuppressWarnings("PMD.TooManyStaticImports")
public class BndBaseliner extends AbstractBndGenerator {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private boolean ignoreMismatched;

    /// Initializes a new bnd baseliner.
    ///
    /// @param project the project
    ///
    public BndBaseliner(Project project) {
        super(project);
    }

    /// Add the instruction specified by key and value.
    ///
    /// @param key the key
    /// @param value the value
    /// @return the bnd baseliner
    ///
    @Override
    public BndBaseliner instruction(String key, String value) {
        super.instruction(key, value);
        return this;
    }

    /// Add the given instructions for the baseliner.
    ///
    /// @param instructions the instructions
    /// @return the bnd baseliner
    ///
    @Override
    public BndBaseliner instructions(Map<String, String> instructions) {
        super.instructions(instructions);
        return this;
    }

    /// Add the instructions from the given bnd (properties) file.
    ///
    /// @param bndFile the bnd file
    /// @return the bnd baseliner
    ///
    @Override
    public BndBaseliner instructions(Path bndFile) {
        super.instructions(bndFile);
        return this;
    }

    /// Ignore mismatches in the baseline evaluation. When invoked,
    /// the [BndBaseliner] will not set the faulty flag on the
    /// [BndBaselineEvaluation] if there are mismatches.
    ///
    /// @return the bnd baseliner
    ///
    public BndBaseliner ignoreMismatches() {
        this.ignoreMismatched = true;
        return this;
    }

    @Override
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    protected <T extends Resource> Stream<T>
            doProvide(ResourceRequest<T> requested) {
        if (!requested.accepts(BndBaselineEvaluationType)) {
            return Stream.empty();
        }

        // Get libraries
        var libraries = newResource(new ResourceType<Resources<
                LibraryJarFile>>() {}).addAll(project().providers(Supply)
                    .resources(project().of(LibraryJarFileType)));
        if (libraries.stream().count() > 1) {
            logger.atWarning().log("More than one library generated by %s,"
                + " baselining can only success for one.", project());
        }
        @SuppressWarnings("unchecked")
        var result = (Stream<T>) libraries.stream().map(this::baseline)
            .filter(Optional::isPresent).map(Optional::get);
        return result;
    }

    private Optional<BndBaselineEvaluation> baseline(LibraryJarFile lib) {
        logger.atFiner().log("Baselining %s in %s", lib, project());

        var groupId = project().<String> get(GroupId);
        var artifactId = Optional.ofNullable(project()
            .<String> get(ArtifactId)).orElse(project().name());
        var version = project().<String> get(Version);
        if (groupId == null) {
            logger.atWarning().log("Cannot baseline in %s without a groupId",
                project());
            return Optional.empty();
        }
        logger.atFinest().log("Baselining %s:%s:%s", groupId, artifactId,
            version);

        // Retrieve previous, relying on version boundaries for selection
        var repoAccess = new MvnRepoLookup().probe().resolve(
            String.format("%s:%s:[0,%s)", groupId, artifactId, version));
        var baselineJar = repoAccess.resources(
            of(MvnRepoLibraryJarFileType)).findFirst();
        if (baselineJar.isEmpty()) {
            return Optional.of(new DefaultBndBaselineEvaluation(
                BndBaselineEvaluationType, project(), lib.path()).name(
                    project().rootProject().relativize(lib.path()).toString())
                    .withBaselineArtifactMissing());
        }
        logger.atFinest().log("Baselining against %s", baselineJar);

        return Optional.of(bndBaseline(baselineJar.get(), lib));
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private BndBaselineEvaluation bndBaseline(LibraryJarFile baseline,
            LibraryJarFile current) {
        try (Processor processor = new Processor();
                Jar baselineJar = new Jar(baseline.path().toFile());
                Jar currentJar = new Jar(current.path().toFile())) {
            applyInstructions(processor);
            DiffPluginImpl differ = new DiffPluginImpl();
            differ.setIgnore(processor.getProperty("-diffignore"));
            Baseline baseliner = new Baseline(processor, differ);

            List<Info> infos = baseliner.baseline(currentJar, baselineJar,
                new Instructions(processor.getProperty("-diffpackages")))
                .stream()
                .sorted(Comparator.comparing(info -> info.packageName))
                .toList();
            BundleInfo bundleInfo = baseliner.getBundleInfo();
            var reportLocation = writeReport(baselineJar, currentJar,
                baseliner, infos, bundleInfo);
            var result = new DefaultBndBaselineEvaluation(
                BndBaselineEvaluationType, project(), baseline.path())
                    .name(bundleInfo.bsn).withReportLocation(reportLocation);
            if (bundleInfo.mismatch && !ignoreMismatched) {
                result.setFaulty().withReason(bundleInfo.reason);
            }
            return result;

        } catch (Exception e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    @SuppressWarnings({ "PMD.AvoidCatchingGenericException",
        "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity" })
    private Path writeReport(Jar baselineJar, Jar currentJar,
            Baseline baseliner, List<Info> infos, BundleInfo bundleInfo) {
        // Copied from gradle plugin and improved
        Path reportLocation = project().buildDirectory().resolve("reports");
        reportLocation.toFile().mkdirs();
        reportLocation = reportLocation.resolve(
            String.format("%s-baseline.txt", currentJar.getName()));
        try (var report = Files.newOutputStream(reportLocation);
                Formatter fmt = new Formatter(report, "UTF-8", Locale.US)) {
            var formatInfo = new FormatInfo(currentJar, baselineJar, bundleInfo,
                infos);
            String format = formatInfo.formatString();
            fmt.format(formatInfo.separatorLine());
            fmt.format(format, " ", "Name", "Type", "Delta", "New", "Old",
                "Suggest", "");
            Diff diff = baseliner.getDiff();
            fmt.format(format, bundleInfo.mismatch ? "*" : " ",
                bundleInfo.bsn, diff.getType(), diff.getDelta(),
                currentJar.getVersion(), baselineJar.getVersion(),
                bundleInfo.mismatch
                    && Objects.nonNull(bundleInfo.suggestedVersion)
                        ? bundleInfo.suggestedVersion
                        : "-",
                "");
            if (bundleInfo.mismatch) {
                fmt.format("%#2S\n", diff);
            }

            if (!infos.isEmpty()) {
                fmt.format(formatInfo.separatorLine());
                fmt.format(format, " ", "Name", "Type", "Delta", "New", "Old",
                    "Suggest", "If Prov.");
                for (Info info : infos) {
                    diff = info.packageDiff;
                    fmt.format(format, info.mismatch ? "*" : " ",
                        diff.getName(), diff.getType(), diff.getDelta(),
                        info.newerVersion,
                        Objects.nonNull(info.olderVersion)
                            && info.olderVersion
                                .equals(aQute.bnd.version.Version.LOWEST)
                                    ? "-"
                                    : info.olderVersion,
                        Objects.nonNull(info.suggestedVersion)
                            && info.suggestedVersion
                                .compareTo(info.newerVersion) <= 0 ? "ok"
                                    : info.suggestedVersion,
                        Objects.nonNull(info.suggestedIfProviders)
                            ? info.suggestedIfProviders
                            : "-");
                    if (info.mismatch) {
                        fmt.format("%#2S\n", diff);
                    }
                }
            }
            fmt.flush();
        } catch (Exception e) {
            throw new BuildException().from(this).cause(e);
        }
        return reportLocation;
    }

    /// The Class FormatInfo.
    ///
    private class FormatInfo {
        private final int maxNameLength;
        private final int maxNewerLength;
        private final int maxOlderLength;

        /// Initializes a new format info.
        ///
        /// @param currentJar the current jar
        /// @param baselineJar the baseline jar
        /// @param bundleInfo the bundle info
        /// @param infos the infos
        /// @throws Exception the exception
        ///
        @SuppressWarnings("PMD.SignatureDeclareThrowsException")
        public FormatInfo(Jar currentJar, Jar baselineJar,
                BundleInfo bundleInfo, List<Info> infos) throws Exception {
            maxNameLength = Math.max(bundleInfo.bsn.length(), infos.stream()
                .map(info -> info.packageDiff.getName().length())
                .sorted(Comparator.reverseOrder()).findFirst().orElse(0));
            maxNewerLength = Math.max(currentJar.getVersion().length(),
                infos.stream()
                    .map(info -> info.newerVersion.toString().length())
                    .sorted(Comparator.reverseOrder()).findFirst().orElse(0));
            maxOlderLength = Math.max(baselineJar.getVersion().length(),
                infos.stream()
                    .map(info -> info.olderVersion.toString().length())
                    .sorted(Comparator.reverseOrder()).findFirst().orElse(0));
        }

        /// Format string.
        ///
        /// @return the string
        ///
        public String formatString() {
            return "%s %-" + maxNameLength + "s %-10s %-10s %-"
                + maxNewerLength + "s %-" + maxOlderLength + "s %-10s %s\n";
        }

        /// Separator string.
        ///
        /// @return the string
        ///
        public String separatorLine() {
            return String.valueOf('=').repeat(
                50 + maxNameLength + maxNewerLength + maxOlderLength) + "\n";
        }
    }

}
