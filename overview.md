# JDrupes Builder

JDrupes Builder ("jdbld" for short) is a
[build automation tool](https://en.wikipedia.org/wiki/Build_system_(software_development))
that uses Java for its configuration and centers around resources.

See the [project's home page](../index.html) for an overview of its features.

@startuml project-provider-classes.svg
interface ResourceProvider
interface ResourceRequest
interface Resource
interface Project
ResourceProvider <|-right- Project : "     "
Project --> "*" ResourceProvider : delegates to
ResourceProvider .down.> Resource
ResourceProvider .down.> ResourceRequest  
@enduml

@startuml single-project-classes.svg
interface ResourceProvider
interface Project
interface Generator
ResourceProvider <|-right- Project : "     "
Project <|.right. SimpleAppProject 
Generator --|> ResourceProvider
SimpleAppProject *--> "*" Generator
@enduml

@startuml simple-appjar-project.svg
scale 1.075

object "project: SimpleAppBuilder" as project
object "compiler: JavaCompiler" as compiler
object "appJarGenerator: UberJarGenerator" as appJarGenerator
project *-right-> compiler : generator
project *--> appJarGenerator : generator
appJarGenerator --> project : provider
@enduml

@startuml build-appjar-project.svg
scale 1.075
hide footbox

actor User as user
user -> project : provide(Resources<AppJarFile>)
activate project
project -> appJarGenerator : provide(Resources<AppJarFile>)
activate appJarGenerator
appJarGenerator -> project : provide(Resources<ClasspathElement>)
activate project
project -> compiler : provide(Resources<ClasspathElement>)
activate compiler
compiler --> project: ClassTree
deactivate compiler
project -> appJarGenerator : ClassTree
deactivate project
appJarGenerator -> project : JarFile
deactivate appJarGenerator
project -> user : JarFile
deactivate project
@enduml

@startuml java-base-types.svg
class ClassTree
class JarFile
class ClasspathElement

ClasspathElement <|-down- JarFile
ClasspathElement <|-down- ClassTree

class Resources<ClasspathElement>
Resources *-left-> ClasspathElement

class RuntimeResources
Resources <|-- RuntimeResources
class CompilationResources
RuntimeResources <|-- CompilationResources
@enduml
