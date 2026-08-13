/*
 * JDrupes Builder
 * Copyright (C) 2025 Michael N. Lipp
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

package org.jdrupes.builder.ext.git;

import com.vdurmont.semver4j.Semver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.CoreProperties;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ProjectVersion;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.core.AbstractGenerator;
import static org.jdrupes.builder.ext.git.GitProperties.*;
import static org.jdrupes.builder.ext.git.GitTypes.*;
import org.jdrupes.gitversioning.api.VersionEvaluator;

/// A generator that creates a new Git tag that denotes a [Project]'s
/// version. The version to be used for the tag is based on
/// the project's [Version property][CoreProperties#Version]. Together
/// with the prefix returned by the prefix evaluator, it forms the tag.
///
/// The generator reads the property `jdbld.versionTagger.mode` to
/// determine how the version for the tag is computed. If the property is not
/// set, the generator uses the version as-is. If set to one of the algorithm
/// constants ([NEXT_MAJOR], [NEXT_MINOR], [NEXT_PATCH], [CLOSEST_MAJOR],
/// [CLOSEST_MINOR], [CLOSEST_PATCH]), it increments the respective
/// version component. The `next` algorithms always increment, while the
/// `closest` algorithms only increment if the version has a pre-release
/// qualifier (usually `SNAPSHOT`). Use the `-P` flag to set the algorithm
/// on the command line, e.g. `jdbld -Pjdbld.versionTagger.mode=nextMinor`.
/// 
/// The generator creates annotated tags with a default message of
/// "Release tag `<tag>`". You can override this message by setting
/// property `jdbld.versionTagger.message`.
///
/// The generator uses an instance of [Git] to access the repository.
/// If also required elsewhere, the jdbld configuration should associate
/// the build project with the instance using [setGitApi]. Else that
/// property will be set on first usage of the generator.
///
public class VersionTagger extends AbstractGenerator {

    /// If defined and not equal to `false` prevents the actual creation
    /// of the tag.
    public static final String DRY_RUN = "jdbld.versionTagger.dryRun";

    /// Defines the message to use when creating the annotated tag.
    private static final String MESSAGE = "jdbld.versionTagger.message";

    /// Defines the evaluation mode for the new version.
    public static final String MODE = "jdbld.versionTagger.mode";

    /// Creates a new tag that increments the major version if the current
    /// version has a pre-release qualifier (e.g. `0.3.1-SNAPSHOT` becomes
    /// `1.0.0`). Does nothing if the version has no snapshot qualifier.
    /// 
    /// Use `jdbld -Pjdbld.versionTagger.mode=closestMajor` to set the algorithm.
    ///
    public static final String CLOSEST_MAJOR = "closestMajor";

    /// Creates a new tag that increments the minor version if the current
    /// version has a pre-release qualifier (e.g. `0.3.1-SNAPSHOT` becomes
    /// `0.4.0`). Does nothing if the version has no snapshot qualifier.
    ///
    /// Use `jdbld -Pjdbld.versionTagger.mode=closestMinor` to set the algorithm.
    ///
    public static final String CLOSEST_MINOR = "closestMinor";

    /// Creates a new tag that increments the patch version if the current
    /// version has a pre-release qualifier (e.g. `0.3.1-SNAPSHOT` becomes
    /// `0.3.2`). Does nothing if the version has no snapshot qualifier.
    ///
    /// Use `jdbld -Pjdbld.versionTagger.mode=closestPatch` to set the algorithm.
    ///
    public static final String CLOSEST_PATCH = "closestPatch";

    /// Creates a new tag that increments the major version regardless of
    /// any qualifier (e.g. `0.3.1` becomes `1.0.0`).
    ///
    /// Use `jdbld -Pjdbld.versionTagger.mode=nextMajor` to set the algorithm.
    ///
    public static final String NEXT_MAJOR = "nextMajor";

    /// Creates a new tag that increments the minor version regardless of
    /// any qualifier (e.g. `0.3.1` becomes `0.4.0`).
    ///
    /// Use `jdbld -Pjdbld.versionTagger.mode=nextMinor` to set the algorithm.
    ///
    public static final String NEXT_MINOR = "nextMinor";

    /// Creates a new tag that increments the patch version regardless of
    /// any qualifier (e.g. `0.3.1` becomes `0.3.2`).
    ///
    /// Use `jdbld -Pjdbld.versionTagger.mode=nextPatch` to set the algorithm.
    ///
    public static final String NEXT_PATCH = "nextPatch";

    private Function<Project, String> prefixEvaluator = _ -> "v";

    private Set<String> preReleaseQualifiers = Set.of("SNAPSHOT");

    /// Sets the property [GitProperties#GitApi] on the root project to
    /// an instance of [Git]. Does nothing if the property is already set.
    ///
    /// @param project the new git api
    /// @return the [Git] instance
    ///
    @SuppressWarnings({ "PMD.AvoidSynchronizedStatement", "PMD.CloseResource" })
    public static Git setGitApi(RootProject project) {
        var git = project.get(GitApi);
        if (git != null) {
            return git;
        }
        synchronized (project) {
            git = project.get(GitApi);
            if (git != null) {
                return git;
            }
            try {
                git = Git.open(project.directory().toFile());
                project.set(GitApi, git);
                return git;
            } catch (IOException e) {
                throw new BuildException().cause(e);
            }
        }

    }

    /// Instantiates a new version reporter.
    ///
    /// @param project the project
    ///
    public VersionTagger(Project project) {
        super(project);
    }

    /// Sets the pre-release qualifiers. Defaults to `"SNAPSHOT"`.
    ///
    /// @param preReleaseQualifier the pre release qualifier
    /// @return the version tagger
    ///
    public VersionTagger preReleaseQualifiers(String... preReleaseQualifier) {
        preReleaseQualifiers
            = new HashSet<>(Arrays.asList(preReleaseQualifier));
        return this;
    }

    /// Sets the function that determines the prefix for the version tag.
    /// The tag is formed by concatenating the prefix with the project's
    /// version. Defaults to `"v"`.
    ///
    /// @param evaluator the function that takes a project and returns the
    /// prefix string
    /// @return this generator for chaining
    ///
    public VersionTagger prefixEvalutor(Function<Project, String> evaluator) {
        this.prefixEvaluator = evaluator;
        return this;
    }

    @Override
    protected <R extends Resource> Collection<R>
            doProvide(ResourceRequest<R> requested) {
        if (!requested.accepts(GitVersionTagType)) {
            return List.of();
        }

        var currentVersion = ProjectVersion.of(project(),
            project().get(CoreProperties.Version)).version();
        String newVersion = evaluateNewVersion(currentVersion);

        var tag = prefixEvaluator.apply(project()) + newVersion;
        @SuppressWarnings("PMD.CloseResource")
        var gitApi = setGitApi(project().rootProject());

        try {
            if (gitApi.tagList().call().stream().map(Ref::getName)
                .anyMatch(name -> name.endsWith("/" + tag))) {
                project().context().out().println(
                    String.format("Tag %s already exists", tag));
            } else {
                // Check prerequitite
                var dirtyFiles = VersionEvaluator.dirtyFiles(
                    gitApi.getRepository(), project().rootProject()
                        .relativize(project().directory()));
                if (!dirtyFiles.isEmpty()) {
                    throw new BuildException().from(this)
                        .message("Won't tag project with dirty files %s",
                            dirtyFiles.stream().map(Path::toString)
                                .collect(Collectors.joining(", ")));
                }

                // Check for dry run
                var dryRunProperty = project().context()
                    .property(DRY_RUN, "false");
                if (!Set.of("", "true", "false").contains(dryRunProperty)) {
                    throw new BuildException().from(this).message("Property "
                        + DRY_RUN + " must be empty or \"true\" or \"false\"");
                }
                if (!dryRunProperty.isEmpty()
                    && !Boolean.parseBoolean(dryRunProperty)) {
                    gitApi.tag().setName(tag).setMessage(project().context()
                        .property(MESSAGE, "Release tag " + tag)).call();
                }
            }
        } catch (GitAPIException e) {
            throw new BuildException().cause(e);
        }
        @SuppressWarnings("unchecked")
        var result = List.of((R) GitVersionTag.of(project(), tag));
        return result;
    }

    private String evaluateNewVersion(String currentVersion) {
        Semver base = new Semver(currentVersion);
        String mode
            = project().context().property(MODE, null);
        boolean isSnapshot = !Collections.disjoint(
            new HashSet<>(Arrays.asList(base.getSuffixTokens())),
            preReleaseQualifiers);
        if (mode == null
            || Set.of(CLOSEST_MAJOR, CLOSEST_MINOR, CLOSEST_PATCH)
                .contains(mode) && !isSnapshot) {
            return currentVersion;
        }

        // Evalute the new version
        switch (mode) {
        case NEXT_MAJOR, CLOSEST_MAJOR -> {
            return base.nextMajor().getValue();
        }
        case NEXT_MINOR, CLOSEST_MINOR -> {
            return base.nextMinor().getValue();
        }
        case NEXT_PATCH, CLOSEST_PATCH -> {
            return base.nextPatch().getValue();
        }
        default -> throw new BuildException().message(
            "Unknown algorithm for deriving new version: %s", mode);
        }
    }
}
