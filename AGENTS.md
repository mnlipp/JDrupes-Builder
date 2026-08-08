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
- **Logging:** Google Flogger. Configured to forward to java.util.logging.
- **Dependencies:** Managed via Maven repositories (lookup and resolution
  handled by the `mvnrepo` module).
- **IDE Support:** Generators for Eclipse (`.project`, `.classpath`, settings)
  and VS Code (`tasks.json`, `launch.json`).

## Building and Running

The project uses its own `jdbld` script for build operations.

### Key Commands

- **Build Project:** `./jdbld -B-x "test-projects/*project*" build`
  (Builds all modules and the application jar in `build/app/`).
- **Run Tests:** `./jdbld -B-x "test-projects/*project*" test`.
- **Generate IDE Config:**
    - Eclipse: `./jdbld -B-x "test-projects/*project*" eclipse`.
    - VS Code: `./jdbld -B-x "test-projects/*project*" vscode`.
- **Generate Documentation:** `./jdbld -B-x "test-projects/*project*" javadoc`.
- **Clean Build:** `./jdbld -B-x "test-projects/*project*" clean`.
- **Help:** `./jdbld -h` or `--help` lists available build aliases. Invoking
  the script with no arguments also shows help.

### Bootstrap Problem

The project builds itself using a chicken-and-egg bootstrap. `./jdbld` fetches
a published JAR from Codeberg (version from `.jdbld.properties`) which is used
to compile `_jdbld/src/jdbld/` and produce a new JAR. To iterate, you must
pass the newly built JAR via `JDBLD_JAR`:

```bash
# First build uses published snapshot
./jdbld -B-x "test-projects/*project*" build
# Subsequent builds use the just-built JAR
JDBLD_JAR=build/app/jdrupes-builder-current.jar \
  ./jdbld -B-x "test-projects/*project*" build
```

Test projects in `test-projects/` must always be excluded with
`-B-x "test-projects/*project*"` when building the tool itself, to avoid them
being treated as part of the build configuration.

## Development Conventions

- **Code Quality:** Rigorous use of Checkstyle (`checkstyle.xml`) and PMD
  (`ruleset.xml`).
- **Style:**
  - Adhere to the existing Java style. Use virtual threads where
    appropriate (as seen in `NpmExecutor`).
  - Line length should not exceed 80 characters.
- **Architecture:**
  - The classes in `api/` define a real API. The classes in `core/` and
    `startup/` implement it. This distinction must be preserved. It must be
    possible to implement the API with another approach if desired.
  - The ResourceProviders and resource types in sub-projects `java/`,
    `mvnrepo/` etc. build on `api/` and `core/`. They are intended to be used
    in build projects, and only exceptionally as basis for other providers.
    Therefore, they don't keep up a distinction between an API and an
    implementation.
- **CLI:** New flags go into `baseOptions()` in
  `startup/.../AbstractLauncher.java`. Help output lives in
  `startup/.../BuildProjectLauncher.printHelp()`.
- **Command Aliases:** Defined via `commandAlias(name).description(desc)`
  fluent builder on `AbstractRootProject`. Descriptions appear in `-h` output.
- **Testing:** Test projects are located in `test-projects/`. JUnit tests for
  core components are in `core/test/`. The project uses JUnit 5.
- **Documentation:** Javadoc comments are written in Markdown (see JEP 467).

## Directory Structure

- `_jdbld/`: The build configuration for the JDrupes-Builder project itself.
- `api/`, `core/`, `java/`, etc.: Source code for the various modules.
- `webpages/`: Project website and documentation (Jekyll-based).
- `test-projects/`: Integration tests and demo projects.
