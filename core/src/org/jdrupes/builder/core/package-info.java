/// A build system is a [ResourcesProvider] of [Resources]. A [ResourcesProvider] of
/// [Resources] typically bases its provisioning on specialized nested
/// [ResourcesProvider]s that handle the provisioning in a particular scope such
/// as a [Project] or by transforming existing [Resources] to new
/// [Resources] by executing a [AbstractGenerator].
/// 
/// The state of a [ResourcesProvider] is updated by adding it to a [Build]. In
/// addition to collecting providers, the [Build] also provides a
/// [Context] that influences how a [ResourcesProvider] evaluates the [Resources].

package org.jdrupes.builder.core;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.ResourcesProvider;
import org.jdrupes.builder.api.Resources;
