# JDrupes-Builder Project Context

JDrupes-Builder (jdbld) is a build automation tool that uses Java code for its
configuration and models builds as collections of resources produced on demand.

## Project Overview

- **Core Concept:** Builds are modeled as a graph of `Project`s and
  `ResourceProvider`s.
- **Configuration:** Instead of XML or YAML, the build logic is written in Java
  within the `_jdbld/src/jdbld/` directory.
- **Key Components:**
    - `api`: Definitions for Projects, Resources, and Intents.
    - `core`: The build engine implementation.
    - `java`, `nodejs`, `junit`, `mvnrepo`, `bnd`: Extensions providing specific
      build capabilities.
    - `startup`: Launchers for the build tool.
- **Intents:** Relationships between projects and providers are qualified by
  Intents:
    - `Supply`: Resources generated specifically for the project (e.g., by a
      Generator).
    - `Consume`: Resources used only by the project's generators.
    - `Reveal`: Resources used by generators and also provided when explicitly
      requested.
    - `Expose`: Resources used by generators and provided to dependents.
    - `Forward`: Resources provided to dependents but not used by the project's
      own generators.

## Technical Stack

- **Language:** Java (Targeting Java 25).
- **Versioning:** Git-based versioning using `org.jdrupes.gitversioning`.
- **Logging:** Google Flogger.
- **Dependencies:** Managed via Maven repositories (lookup and resolution
  handled by the `mvnrepo` module).
- **IDE Support:** Generators for Eclipse (`.project`, `.classpath`, settings)
  and VS Code (`tasks.json`, `launch.json`).

## Building and Running

The project uses its own `jdbld` script for build operations.

### Key Commands

- **Build Project:** `./jdbld build` (Builds all modules and the application jar
  in `build/app/`).
- **Run Tests:** `./jdbld test`.
- **Generate IDE Config:**
    - Eclipse: `./jdbld eclipse`.
    - VS Code: `./jdbld vscode`.
- **Generate Documentation:** `./jdbld javadoc`.
- **Clean Build:** `./jdbld clean`.

Note: To build the builder using the version under development, you must
specify the jar created by `./jdbld build` as follows:
`JDBLD_JAR=build/app/jdrupes-builder-current.jar ./jdbld build`.
To avoid confusion with the test projects in `test-projects/`, these
must be excluded from the build with the option `-B-x "test-projects/*project*"`.
So invocations of the build script look like this:
`JDBLD_JAR=build/app/jdrupes-builder-current.jar ./jdbld -B-x "test-projects/*project*" -B-x "test-projects/*project*" ...`.

## Development Conventions

- **Code Quality:** Rigorous use of Checkstyle (`checkstyle.xml`) and PMD
  (`ruleset.xml`).
- **Style:**
  - Adhere to the existing Java style. Use virtual threads where
    appropriate (as seen in `NpmExecutor`).
  - Line length should not exceed 80 characters.
- **Architecture:**
  - The classes in api/ define a real API. The classes in core/ and startup/
    implement it. This distinction must be preserved. It must be possible
    to implement the API with another approach if desired. 
  - The ResourceProviders and resource types in sub-projects java/, mvnrepo/
    etc. build on api/ and core/. They are intended to be used in build
    projects, and only exceptionally as basis for other providers. Therefore,
    they don't keep up a distinction between an API and an implementation.
- **Testing:** Test projects are located in `test-projects/`. JUnit tests for
  core components are in `core/test/`. The project uses JUnit 5.
- **Documentation:** Javadoc comments are written in Markdown (see JEP 467).

## Directory Structure

- `_jdbld/`: The build configuration for the JDrupes-Builder project itself.
- `api/`, `core/`, `java/`, etc.: Source code for the various modules.
- `webpages/`: Project website and documentation (Jekyll-based).
- `test-projects/`: Integration tests and demo projects.

## Documentation style

The style used for the documentation in webpages/ should remain consistent.

 
