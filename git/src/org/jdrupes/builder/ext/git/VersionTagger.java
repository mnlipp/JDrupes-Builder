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

package org.jdrupes.builder.ext.git;

import com.vdurmont.semver4j.Semver;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
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
/// set, the generator removes any pre-release qualifier (usually `SNAPSHOT`)
/// from the version if present and otherwise uses the version as-is.
/// 
/// If the property is set to one of the algorithm constants
/// ([NEXT_MAJOR], [NEXT_MINOR], [NEXT_PATCH], [INCREMENT_MAJOR],
/// [INCREMENT_MINOR]), it increments the respective
/// version component. The `next` algorithms always increment, while the
/// `increment` algorithms only increment if the version has a pre-release
/// qualifier. Use the `-P` flag to set the algorithm on the command line,
/// e.g. `jdbld -Pjdbld.versionTagger.mode=incrementMinor`.
/// 
/// The supported algorithms follow this usage pattern: every change
/// starts with a patch version increment plus a pre-release qualifier
/// (e.g. `1.0.1-SNAPSHOT`). If development reveals the change warrants a
/// higher version, create a lightweight tag with the intended minor or
/// major version (e.g. `git tag 1.1.0-SNAPSHOT`). If at release time,
/// it becomes clear that a new major or minor version is needed for
/// several sub-projects, use the `increment` algorithms to force the
/// desired version bump.
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
/// This provider is made available as an extension.
/// [![org.jdrupes:jdbld-ext-git:](
/// https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcodeberg.org%2Fapi%2Fpackages%2FJDrupes%2Fmaven%2Forg%2Fjdrupes%2Fjdbld-ext-git%2Fmaven-metadata.xml&strategy=releaseProperty)
/// ](https://codeberg.org/JDrupes/-/packages/maven/org.jdrupes:jdbld-ext-git/versions)
/// 
@SuppressWarnings("PMD.GodClass")
public class VersionTagger extends AbstractGenerator {

    /// If defined and not equal to `false` prevents the actual creation
    /// of the tag.
    public static final String DRY_RUN = "jdbld.versionTagger.dryRun";

    /// Defines the message to use when creating the annotated tag.
    private static final String MESSAGE = "jdbld.versionTagger.message";

    /// Defines the evaluation mode for the new version.
    public static final String MODE = "jdbld.versionTagger.mode";

    /// Creates a new tag by removing the pre-release qualifier
    /// (e.g. `0.3.1-SNAPSHOT` becomes `0.3.1`). Does nothing if the
    /// version has no snapshot qualifier. This is the default behavior
    /// if no algorithm is specified. 
    /// 
    /// Use `jdbld -Pjdbld.versionTagger.mode=release` to set the algorithm.
    ///
    public static final String RELEASE = "release";

    /// Creates a new tag that removes any pre-release qualifier and
    /// increments the major version unless the version without qualifier
    /// is already a new major version (e.g. `0.3.1-SNAPSHOT` becomes `1.0.0`,
    /// `1.0.0-SNAPSHOT` becomes `1.0.0`). Does nothing if the version has
    /// no snapshot qualifier.
    /// 
    /// Use
    /// `jdbld -Pjdbld.versionTagger.mode=incrementMajor` to set the algorithm.
    ///
    public static final String INCREMENT_MAJOR = "incrementMajor";

    /// Creates a new tag that removes any pre-release qualifier and
    /// increments the minor version unless the version without qualifier
    /// is already a new minor version (e.g. `0.3.1-SNAPSHOT` becomes `0.4.0`,
    /// `0.4.0-SNAPSHOT` becomes `0.4.0`). Does nothing if the version has
    /// no snapshot qualifier.
    /// 
    /// Use
    /// `jdbld -Pjdbld.versionTagger.mode=incrementMajor` to set the algorithm.
    ///
    public static final String INCREMENT_MINOR = "incrementMinor";

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
    @SuppressWarnings({ "PMD.CloseResource", "PMD.AvoidSynchronizedStatement",
        "PMD.AvoidDuplicateLiterals" })
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
    public VersionTagger prefixEvaluator(Function<Project, String> evaluator) {
        this.prefixEvaluator = evaluator;
        return this;
    }

    @Override
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.AvoidInstanceofChecksInCatchClause" })
    protected <R extends Resource> Collection<R>
            doProvide(ResourceRequest<R> requested) {
        if (!requested.accepts(GitVersionTagType)) {
            return List.of();
        }

        var currentVersion = ProjectVersion.of(project(),
            project().get(CoreProperties.Version)).version();
        String newVersion = evaluateNewVersion(currentVersion);
        var tag = prefixEvaluator.apply(project()) + newVersion;
        var gitApi = setGitApi(project().rootProject());

        // Check for existing tag
        try {
            var existing = findTag(gitApi, tag);
            if (existing.isPresent()) {
                @SuppressWarnings("unchecked")
                var result = List.of((R) existing.get());
                return result;
            }
        } catch (GitAPIException e) {
            throw new BuildException().from(this).cause(e);
        }

        // Create new tag
        for (int attempt = 0;; attempt++) {
            try {
                return createTag(newVersion, tag, gitApi);
            } catch (GitAPIException e) {
                if (e instanceof ConcurrentRefUpdateException && attempt < 10) {
                    try {
                        Thread.sleep(10L * (attempt + 1));
                        continue;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                throw new BuildException().cause(e);
            }
        }
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private Optional<GitVersionTag> findTag(Git gitApi, String tag)
            throws GitAPIException {
        Optional<Ref> existing;
        synchronized (gitApi.getRepository()) {
            existing = gitApi.tagList().call().stream().filter(
                ref -> ref.getName().endsWith("/" + tag)).findFirst();
        }
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(GitVersionTag.of(project(), tag,
            resolveTagTimestamp(gitApi, existing.get())));
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private Instant resolveTagTimestamp(Git gitApi, Ref tagRef) {
        if (tagRef == null) {
            return Instant.now();
        }
        synchronized (gitApi.getRepository()) {
            try (var walk = new RevWalk(gitApi.getRepository())) {
                var obj = walk.parseAny(tagRef.getObjectId());
                if (obj instanceof RevTag revTag) {
                    var taggerIdent = revTag.getTaggerIdent();
                    if (taggerIdent != null) {
                        return taggerIdent.getWhenAsInstant();
                    }
                }
                if (obj instanceof RevCommit revCommit) {
                    return revCommit.getCommitterIdent().getWhenAsInstant();
                }
                return Instant.now();
            } catch (IOException e) {
                throw new BuildException().cause(e);
            }
        }
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private <R extends Resource> List<R> createTag(String newVersion,
            String tag, Git gitApi) throws GitAPIException {
        // Check prerequisite
        var dryRun = checkDryRun();
        if (!checkPrerequisites(gitApi, newVersion, tag, dryRun)) {
            return Collections.emptyList();
        }

        // Create tag unless dry run
        if (!dryRun) {
            try {
                synchronized (gitApi.getRepository()) {
                    gitApi.tag().setName(tag).setMessage(project().context()
                        .property(MESSAGE, "Release tag " + tag)).call();
                }
            } catch (RefAlreadyExistsException e) {
                var existing = findTag(gitApi, tag);
                if (existing.isEmpty()) {
                    throw new BuildException().from(this).cause(e).message(
                        "Tag %s reported to exist but not found", tag);
                }
                project().context().out().println(
                    String.format("Tag %s already exists", tag));
                @SuppressWarnings("unchecked")
                var result = List.of((R) existing.get());
                return result;
            }
        }
        @SuppressWarnings("unchecked")
        var result
            = List.of((R) GitVersionTag.of(project(), tag, Instant.now()));
        return result;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private boolean checkPrerequisites(Git gitApi, String newVersion,
            String tag, boolean dryRun) throws GitAPIException {
        List<Path> dirtyFiles;
        synchronized (gitApi.getRepository()) {
            var evaluator = VersionEvaluator
                .forRepository(gitApi.getRepository())
                .subDirectory(project().directory());
            dirtyFiles = evaluator.dirtyFiles().toList();
        }
        if (!dirtyFiles.isEmpty()) {
            if (dryRun) {
                project().context().out().println(
                    String.format("%s: wouldn't create tag %s "
                        + "since project has dirty files %s", this,
                        tag, dirtyFiles.size() > 5
                            ? dirtyFiles.stream().limit(5).map(Path::toString)
                                .collect(Collectors.joining(", ")) + ", ..."
                            : dirtyFiles.stream().map(Path::toString)
                                .collect(Collectors.joining(", "))));
                return false;
            } else {
                throw new BuildException().from(this)
                    .message("Won't create tag %s since project has "
                        + "dirty files %s", tag,
                        dirtyFiles.stream().map(Path::toString)
                            .collect(Collectors.joining(", ")));
            }
        }
        if (isPreRelease(newVersion)) {
            if (dryRun) {
                project().context().out().println(
                    String.format("%s: wouldn't create tag %s "
                        + "since it is a pre-release", this, tag));
                return false;
            } else {
                throw new BuildException().from(this)
                    .message("Won't create tag %s since it is a pre-release",
                        tag);
            }
        }
        return true;
    }

    private boolean checkDryRun() {
        var dryRunProperty = project().context()
            .property(DRY_RUN, "false");
        if (!Set.of("", "true", "false").contains(dryRunProperty)) {
            throw new BuildException().from(this).message("Property "
                + DRY_RUN + " must be empty or \"true\" or \"false\"");
        }
        return dryRunProperty.isEmpty()
            || Boolean.parseBoolean(dryRunProperty);
    }

    private String evaluateNewVersion(String currentVersion) {
        Semver base = new Semver(currentVersion);
        String mode = project().context().property(MODE, RELEASE);
        if (Set.of(RELEASE, INCREMENT_MAJOR, INCREMENT_MINOR)
            .contains(mode) && !isPreRelease(currentVersion)) {
            return currentVersion;
        }

        // Evaluate the new version
        switch (mode) {
        case RELEASE -> {
            return base.withClearedSuffix().getValue();
        }
        case INCREMENT_MAJOR -> {
            if (base.getMinor() == 0 && base.getPatch() == 0) {
                return base.withClearedSuffix().getValue();
            }
            return base.nextMajor().getValue();
        }
        case INCREMENT_MINOR -> {
            if (base.getPatch() == 0) {
                return base.withClearedSuffix().getValue();
            }
            return base.nextMinor().getValue();
        }
        case NEXT_MAJOR -> {
            return base.nextMajor().getValue();
        }
        case NEXT_MINOR -> {
            return base.nextMinor().getValue();
        }
        case NEXT_PATCH -> {
            return base.nextPatch().getValue();
        }
        default -> throw new BuildException().message(
            "Unknown algorithm for deriving new version: %s", mode);
        }
    }

    private boolean isPreRelease(String version) {
        Semver base = new Semver(version);
        return !Collections.disjoint(
            new HashSet<>(Arrays.asList(base.getSuffixTokens())),
            preReleaseQualifiers);
    }
}
