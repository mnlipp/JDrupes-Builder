# JDrupes Builder

JDrupes Builder ("jdbld" for short) is a
[build automation tool](https://en.wikipedia.org/wiki/Build_system_(software_development))
that centers around resources and uses Java for its configuration.

In jdbld's terminology, a build system is a
[provider][org.jdrupes.builder.api.ResourceProvider] for
[org.jdrupes.builder.api.Resource]s. The build system's configuration
is structured using [org.jdrupes.builder.api.Project]s.
In a single project configuration, a project simply provides the resources
that it's [org.jdrupes.builder.api.Generator]s provide.

![Builder classes](single-project-classes.svg)

Using this API, you can create a simple build configuration.

![Simple app jar project](simple-appjar-project.svg)

The project contains two generators. The first is a
[org.jdrupes.builder.java.JavaCompiler], which provides resources of type
[org.jdrupes.builder.java.ClassTree] to the project. The second
generator is an [org.jdrupes.builder.java.AppJarBuilder]. This application
jar builder provides resources of type [org.jdrupes.builder.java.JarFile].
In order to build the jar, it uses the [org.jdrupes.builder.java.ClassTree]
resources provided by the project.

The actual build configuration looks like this:

```java
public class SimpleApp extends AbstractProject {

    public SimpleApp() {
        super(name("simple-app"));
        generator(JavaCompiler::new).addSources(Path.of("src"), "**/*.java");
        generator(AppJarBuilder::new).add(this)
            .mainClass("jdbld.demo.simpleapp.App");
    }
}
```


![Simple app jar project](build-appjar-project.svg)


The focus on resources usually works. It fails when you want
the builder to do something that cannot really be described as
creating a resource. The most prominent example of this is probably
cleaning a build. What does this provide? Well, how about "cleanliness"?
Depending on your point of view, "cleanliness" may be the absence of
something, but you could also argue that "cleanliness"
provides something.

@startuml single-project-classes.svg
interface ResourceProvider
interface Project
Project -up-|> ResourceProvider
interface Generator
Generator -up-|> ResourceProvider
Project *-right-> Generator 
@enduml

@startuml simple-appjar-project.svg
object "simpleApp: Project" as project
object "compiler: JavaCompiler" as compiler
object "appJarBuilder: AppJarBuilder" as appJarBuilder
project *-right-> compiler : <<generator>>
project *--> appJarBuilder : <<generator>>
appJarBuilder --> project : <<provider>>
@enduml

@startuml build-appjar-project.svg
hide footbox

actor User as user
user -> project : provide(AppJarFileType)
project -> appJarBuilder : provide(AppJarFileType)
appJarBuilder -> project : provide(ClasspathElement)
@enduml
