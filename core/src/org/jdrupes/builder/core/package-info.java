/// A build system is a [Provider] of [Resources]. A [Provider] of
/// [Resources] typically bases its provisioning on specialized nested
/// [Provider]s that handle the provisioning in a particular scope such
/// as a [Project] or by transforming existing [Resources] to new
/// [Resources] by executing a [AbstractTask].
/// 
/// The state of a [Provider] is updated by adding it to a [Build]. In
/// addition to collecting providers, the [Build] also provides a
/// [Context] that influences how a [Provider] evaluates the [Resources].
package org.jdrupes.builder.core;

import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Provider;
import org.jdrupes.builder.api.Resources;
