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

package org.jdrupes.builder.mvnrepo;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.graph.transformer.ConfigurableVersionSelector;
import org.jdrupes.builder.api.BuildException;

/// Manages a global instance of [RepositorySystem] and 
/// [RepositorySystemSession] and provides lists of [RemoteRepository]s
/// from profiles in `settings.xml`.
///
public final class MavenContext {

    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile SessionData theSession;
    private static final RemoteRepository MAVEN_CENTRAL_REPO
        = new RemoteRepository.Builder("central", "default",
            "https://repo.maven.apache.org/maven2")
                .setReleasePolicy(
                    createDefaultPolicy(MvnVersionType.RELEASE, true))
                .setSnapshotPolicy(
                    createDefaultPolicy(MvnVersionType.SNAPSHOT, false))
                .build();

    private MavenContext() {
    }

    private record SessionData(Settings settings,
            RepositorySystem repositorySystem,
            RepositorySystemSession repositorySession) {
    }

    /// Returns the singleton, lazily created session data.
    ///
    /// @return the session data
    ///
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    private static SessionData session() {
        if (theSession != null) {
            return theSession;
        }
        synchronized (MavenContext.class) {
            if (theSession != null) {
                return theSession;
            }
            return initSession();
        }
    }

    private static SessionData initSession() {
        // Settings
        SettingsBuildingRequest settingsRequest
            = new DefaultSettingsBuildingRequest().setUserSettingsFile(
                new File(System.getProperty("user.home"), ".m2/settings.xml"));
        SettingsBuilder settingsBuilder
            = new DefaultSettingsBuilderFactory().newInstance();
        SettingsBuildingResult settingsResult;
        try {
            settingsResult = settingsBuilder.build(settingsRequest);
        } catch (SettingsBuildingException e) {
            throw new BuildException().cause(e);
        }
        var settings = settingsResult.getEffectiveSettings();

        // Repository system
        @SuppressWarnings("PMD.CloseResource")
        var repoSystem = new RepositorySystemSupplier().get();

        // Repository system session
        String localRepoPath = settings.getLocalRepository() != null
            ? settings.getLocalRepository()
            : System.getProperty("user.home") + "/.m2/repository";
        @SuppressWarnings("PMD.CloseResource")
        var session = new SessionBuilderSupplier(repoSystem).get()
            .withLocalRepositoryBaseDirectories(Path.of(localRepoPath))
            .setConfigProperty(
                ConfigurableVersionSelector.CONFIG_PROP_SELECTION_STRATEGY,
                ConfigurableVersionSelector.HIGHEST_SELECTION_STRATEGY)
            .build();

        // Combine
        theSession = new SessionData(settings, repoSystem, session);
        return theSession;
    }

    /// Repository system.
    ///
    /// @return the repository system
    ///
    public static RepositorySystem repositorySystem() {
        return session().repositorySystem();
    }

    /// Repository session.
    ///
    /// @return the repository system session
    ///
    public static RepositorySystemSession repositorySession() {
        return session().repositorySession();
    }

    /// Looks up the credentials for the specified server in `settings.xml`.
    /// Invokes the consumer with the username and password if found.
    ///
    /// @param serverId the server id
    /// @param consumer the consumer
    /// @return true, if found
    ///
    public static boolean lookupCredentials(String serverId,
            BiConsumer<String, String> consumer) {
        return session().settings().getServers().stream()
            .filter(s -> serverId.equals(s.getId())).findFirst().map(s -> {
                consumer.accept(s.getUsername(), s.getPassword());
                return true;
            }).orElse(false);
    }

    /// Returns the [RemoteRepository] for Maven Central.
    ///
    /// @return the remote repository
    ///
    public static RemoteRepository mavenCentral() {
        return MAVEN_CENTRAL_REPO;
    }

    /// Return the [RemoteRepository]s from the specified profile.
    ///
    /// @param profileId the profile id
    /// @return the repositories
    ///
    public static List<RemoteRepository> repositories(String profileId) {
        Objects.requireNonNull(profileId);
        List<RemoteRepository> repos = new ArrayList<>();
        var settings = session().settings();
        if (!settings.getActiveProfiles().contains(profileId)) {
            return repos;
        }
        Map<String, Profile> profiles = settings.getProfilesAsMap();
        Profile profile = profiles.get(profileId);
        if (profile == null) {
            return repos;
        }
        for (Repository repo : profile.getRepositories()) {
            @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
            var builder = new RemoteRepository.Builder(repo.getId(),
                "default", repo.getUrl())
                    .setReleasePolicy(createPolicy(MvnVersionType.RELEASE,
                        repo.getReleases()))
                    .setSnapshotPolicy(createPolicy(MvnVersionType.SNAPSHOT,
                        repo.getSnapshots()));
            repos.add(builder.build());
        }
        return repos;
    }

    /// Creates a policy for the specified type with reasonable defaults.
    ///
    /// @param type the type
    /// @param enabled the enabled
    /// @return the repository policy
    ///
    /* default */ static RepositoryPolicy createDefaultPolicy(
            MvnVersionType type, boolean enabled) {
        return createPolicy(type, enabled, null, null);
    }

    /// Creates a policy from settings data. Fills in reasonable defaults if
    /// necessary.
    ///
    /// @param type the type
    /// @param policy the policy data from settings
    /// @return the repository policy
    ///
    /* default */ static RepositoryPolicy createPolicy(MvnVersionType type,
            org.apache.maven.settings.RepositoryPolicy policy) {
        if (policy == null) {
            return createPolicy(type, false, null, null);
        }
        return createPolicy(type, policy.isEnabled(),
            policy.getUpdatePolicy(), policy.getChecksumPolicy());
    }

    /// Creates a policy from settings data with the given details.
    /// Fills in reasonable defaults for `null` values.
    ///
    /// @param type the type
    /// @param enabled the enabled
    /// @param updatePolicy the update policy
    /// @param checksumPolicy the checksum policy
    /// @return the repository policy
    ///
    /* default */ static RepositoryPolicy createPolicy(MvnVersionType type,
            boolean enabled, String updatePolicy, String checksumPolicy) {
        if (updatePolicy == null) {
            updatePolicy = type == MvnVersionType.SNAPSHOT
                ? RepositoryPolicy.UPDATE_POLICY_ALWAYS
                : RepositoryPolicy.UPDATE_POLICY_NEVER;
        }
        if (checksumPolicy == null) {
            checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        }
        return new RepositoryPolicy(enabled, updatePolicy, checksumPolicy);
    }
}
