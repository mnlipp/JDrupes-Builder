# JDrupes Builder

A [build automation tool](https://en.wikipedia.org/wiki/Build_system_(software_development))
that focuses on resources and uses Java for its configuration.

A build system is a [provider][org.jdrupes.builder.api.ResourceProvider]
for [org.jdrupes.builder.api.Resource]s. The build system's configuration
typically bases the provisioning on specialized nested
[org.jdrupes.builder.api.ResourceProvider]s that handle the provisioning
in a particular scope such as a [org.jdrupes.builder.api.Project] or by
transforming existing [org.jdrupes.builder.api.Resource]s to new
[org.jdrupes.builder.api.Resource]s by executing a
[org.jdrupes.builder.api.Generator].



The focus on resources usually works. It fails when you want
the builder to do something that cannot really be described as
creating a resource. The most prominent example of this is probably
cleaning a build. What does this provide? Well, how about "cleanliness"?
Depending on your point of view, "cleanliness" may be the absence of
something, but you could also argue that "cleanliness"
provides something.