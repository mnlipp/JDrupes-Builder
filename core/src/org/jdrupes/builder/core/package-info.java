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

/// A build system is a [ResourceProvider] for [Resource]s.
/// A [ResourceProvider] of [Resource]s typically bases its provisioning
/// on specialized nested [ResourceProvider]s that handle the provisioning
/// in a particular scope such as a [Project] or by transforming existing
/// [Resource]s to new [Resource]s by executing a [Generator].
/// 
/// The state of a [ResourceProvider] is updated by adding it to a [Build].

package org.jdrupes.builder.core;

import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
