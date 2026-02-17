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

package org.jdrupes.builder.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.Cleanliness;
import org.jdrupes.builder.api.ConfigurationException;
import org.jdrupes.builder.api.NamedParameter;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.RootProject;

/// The Class AbstractRootProject.
///
public abstract class AbstractRootProject extends AbstractProject
        implements RootProject {

    /* default */ @SuppressWarnings("PMD.FieldNamingConventions")
    static final ScopedValue<
            AbstractRootProject> scopedRootProject = ScopedValue.newInstance();
    private final Map<String, CommandData> commands;
    private final Map<Class<? extends Project>, Future<Project>> projects;

    /// Initializes a new abstract root project.
    ///
    /// @param params the params
    ///
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public AbstractRootProject(NamedParameter<?>... params) {
        super(params);
        // ConcurrentHashMap does not support null values.
        projects = Collections.synchronizedMap(new HashMap<>());
        commands = new HashMap<>();
        commandAlias("clean").resources(of(Cleanliness.class));
    }

    @Override
    public void close() {
        if (this instanceof RootProject) {
            context().close();
        }
    }

    @Override
    public RootProject rootProject() {
        return this;
    }

    @Override
    public Project project(Class<? extends Project> prjCls) {
        if (this.getClass().equals(prjCls)) {
            return this;
        }
        try {
            return projects.computeIfAbsent(prjCls, this::futureProject)
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BuildException().from(this).cause(e);
        }
    }

    private Future<Project> futureProject(Class<? extends Project> prjCls) {
        @SuppressWarnings("PMD.CloseResource")
        final DefaultBuildContext context = context();
        return context().executor().submit(() -> {
            return context.call(() -> createProject(prjCls));
        });
    }

    private Project createProject(Class<? extends Project> prjCls) {
        try {
            return ScopedValue
                .where(scopedRootProject, this)
                .call(() -> (Project) prjCls.getConstructor()
                    .newInstance());
        } catch (SecurityException | ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public RootProject.CommandBuilder commandAlias(String name) {
        if (!(this instanceof RootProject)) {
            throw new ConfigurationException().from(this).message(
                "Commands can only be defined for the root project.");
        }
        return new CommandBuilder((RootProject) this, name);
    }

    /// The Class CommandBuilder.
    ///
    public class CommandBuilder implements RootProject.CommandBuilder {
        private final RootProject rootProject;
        private final String name;
        private String projects = "";

        /// Initializes a new command builder.
        ///
        /// @param rootProject the root project
        /// @param name the name
        ///
        public CommandBuilder(RootProject rootProject, String name) {
            this.rootProject = rootProject;
            this.name = name;
        }

        /// Projects.
        ///
        /// @param projects the projects
        /// @return the root project. command builder
        ///
        @Override
        public RootProject.CommandBuilder projects(String projects) {
            this.projects = projects;
            return this;
        }

        /// Resources.
        ///
        /// @param requests the requests
        /// @return the root project
        ///
        @Override
        public RootProject resources(ResourceRequest<?>... requests) {
            for (int i = 0; i < requests.length; i++) {
                if (requests[i].uses().isEmpty()) {
                    requests[i] = requests[i].usingAll();
                }
            }
            commands.put(name, new CommandData(projects, requests));
            return rootProject;
        }
    }

    /// The Record CommandData.
    ///
    /// @param pattern the pattern
    /// @param requests the requests
    ///
    public record CommandData(String pattern, ResourceRequest<?>... requests) {
    }

    /// Lookup command.
    ///
    /// @param name the name
    /// @return the command data
    ///
    public CommandData lookupCommand(String name) {
        return commands.getOrDefault(name,
            new CommandData("", new ResourceRequest[0]));
    }

}
