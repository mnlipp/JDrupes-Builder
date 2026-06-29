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

import com.google.common.flogger.FluentLogger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.eclipse.aether.artifact.Artifact;
import org.jdrupes.builder.api.BuildContext;

/// The base class for all Maven publishing destinations.
///
/// It provides common functionality for managing repository credentials,
/// supporting fallbacks to build context properties and Maven's `settings.xml`,
/// and defining which [MvnVersionType] (SNAPSHOT or RELEASE) the destination
/// accepts.
///
public abstract class MvnPublishingDestination {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final Set<MvnVersionType> acceptedTypes;
    private String repoUser;
    private String repoPass;
    private String id;

    /// Initializes a new Maven publishing destination.
    ///
    /// @param publicationTypes the accepted publication types
    ///
    public MvnPublishingDestination(MvnVersionType... publicationTypes) {
        acceptedTypes = EnumSet.copyOf(Arrays.asList(publicationTypes));
    }

    /// Checks if the given publication type is accepted.
    ///
    /// @param type the type
    /// @return true, if successful
    ///
    public boolean accepts(MvnVersionType type) {
        return acceptedTypes.contains(type);
    }

    /// Sets the id.
    ///
    /// @param id the new id
    /// @return this destination
    ///
    @SuppressWarnings("PMD.ShortMethodName")
    public MvnPublishingDestination id(String id) {
        this.id = id;
        return this;
    }

    ///
    /// Returns the id.
    /// 
    /// @return the id
    /// 
    @SuppressWarnings("PMD.ShortMethodName")
    public String id() {
        return id;
    }

    /// Sets the Maven repository credentials.
    ///
    /// @param user the user name
    /// @param pass the password
    /// @return this destination
    ///
    public MvnPublishingDestination credentials(String user, String pass) {
        logger.atConfig().log("Using explicitly set credentials for %s", this);
        this.repoUser = user;
        this.repoPass = pass;
        return this;
    }

    /// Returns the repository user set by [credentials] or a fallback.
    /// 
    /// The fallback order is:
    /// 
    /// 1. Look for properties `mvnrepo.user` and `mvnrepo.password`
    ///    in the properties provided by the [BuildContext].
    /// 
    /// 2. If an id is set, look for the user and password in the
    ///    `servers` section with this id in the Maven `settings.xml`.
    ///
    /// @param context the context
    /// @return the user
    ///
    protected String repositoryUser(BuildContext context) {
        fillInCredentials(context);
        return repoUser;
    }

    /// Returns the repository password set by [credentials] or a fallback.
    /// See [repositoryUser] for the fallback logic.
    ///
    /// @param context the context
    /// @return the password
    ///
    protected String repositoryPassword(BuildContext context) {
        fillInCredentials(context);
        return repoPass;
    }

    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void fillInCredentials(BuildContext context) {
        if (repoUser != null) {
            return;
        }

        // Try properties
        var user = context.property("mvnrepo.user", null);
        if (user != null) {
            logger.atConfig().log(
                "Using credentials from properties for %s", this);
            repoUser = user;
            repoPass = context.property("mvnrepo.password", null);
            return;
        }

        // Try settings
        if (id != null && MavenContext.lookupCredentials(id, (u, p) -> {
            logger.atConfig().log(
                "Using credentials from settings for %s", this);
            repoUser = u;
            repoPass = p;
        })) {
            return;
        }

        // Fallback
        repoUser = "";
        repoPass = "";
    }

    /* default */ abstract void publish(BuildContext context,
            MvnPublisher publisher, Artifact mainArtifact,
            List<Artifact> toDeploy);
}
