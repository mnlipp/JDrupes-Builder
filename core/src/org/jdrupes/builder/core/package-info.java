/// A build system is a [ResourceProvider] for [Resource]s.
/// A [ResourceProvider] of [Resource]s typically bases its provisioning
/// on specialized nested [ResourceProvider]s that handle the provisioning
/// in a particular scope such as a [Project] or by transforming existing
/// [Resource]s to new [Resource]s by executing a [Generator].
/// 
/// The state of a [ResourceProvider] is updated by adding it to a [Build].
/// In addition to collecting providers, the [Build] also provides a
/// [Context] that influences how a [ResourceProvider] evaluates the
/// requested [Resource]s.

package org.jdrupes.builder.core;

import org.jdrupes.builder.api.Build;
import org.jdrupes.builder.api.Generator;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceProvider;
