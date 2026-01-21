# JDrupes Builder

JDrupes Builder (jdbld) is a
[build automation tool](https://en.wikipedia.org/wiki/Build_system_(software_development))
that uses Java code for its configuration and models builds as collections
of resources that are produced on demand.

See the [project's home page](../index.html) for an overview of its features.

@startuml project-provider-classes.svg
interface ResourceProvider
interface ResourceRequest
interface Resource
interface Project
ResourceProvider <|-left- Project : "     "
Project --> "*" ResourceProvider : delegates to
ResourceProvider .down.> Resource
ResourceProvider .down.> ResourceRequest
@enduml

@startuml single-project-classes.svg
interface ResourceProvider
interface Project
interface Generator
ResourceProvider <|-left- Project
Project <|.up. SimpleAppProject 
Generator -right-|> ResourceProvider
SimpleAppProject *-right-> "*" Generator
@enduml

@startuml simple-appjar-project.svg
scale 1.075

object "project: SimpleAppBuilder" as project
object "compiler: JavaCompiler" as compiler
object "appJarGenerator: UberJarGenerator" as appJarGenerator
project *-right-> compiler : generator
project *--> appJarGenerator : generator
appJarGenerator --> project : provide to
compiler --> project : provide to
@enduml

@startuml build-appjar-project.svg
scale 1.075
hide footbox

actor User as user
user -> project : provide(AppJarFile)
activate project
project -> appJarGenerator : provide(AppJarFile)
activate appJarGenerator
appJarGenerator -> project : provide(ClasspathElement)
activate project
project -> compiler : provide(ClasspathElement)
activate compiler
compiler --> project: ClassTree
deactivate compiler
project -> appJarGenerator : ClassTree
deactivate project
appJarGenerator -> project : AppJarFile
deactivate appJarGenerator
project -> user : AppJarFile
deactivate project
@enduml

@startuml java-base-types.svg
class ClassTree
class JarFile
interface ClasspathElement

ClasspathElement <|.down. JarFile
ClasspathElement <|.down. ClassTree
@enduml
