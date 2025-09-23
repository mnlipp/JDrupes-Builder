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

package org.jdrupes.builder.mvnrepo;

import java.util.Optional;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceFactory;
import org.jdrupes.builder.api.ResourceType;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

/// A factory for creating Java related resource objects.
///
public class MvnRepoResourceFactory implements ResourceFactory {

    /// Instantiates a new java resource factory.
    ///
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public MvnRepoResourceFactory() {
        // Make javadoc happy
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Resource> Optional<T> newResource(ResourceType<T> type,
            Project project, Object... args) {
        if (MvnRepoDependencyType.isAssignableFrom(type)) {
            return Optional.of((T) new DefaultMvnRepoDependency((String) args[0]));
        }
        return Optional.empty();
    }

}
