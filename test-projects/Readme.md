# Test projects

## Structure of test projects

This folder contains test projects for the JDrupes Builder and the 
directives for building them and running the tests. The build configuration
makes use jdbld's merge feature, so think of `/_jdbld/src` and
`test-projects/_jdbld/src` as the same folder.

The individual test project's building configuration is a simplified
variant of the `BootstrapBuild` which compiles the sources and resources
from the test project's `_jdbld` folder. It adds, however, the test cases
found in `_jdbld/test` to the build. The resulting classes are then
used to run the test cases.

All test projects are valid jdbld projects, i.e. they can also be build
from the command line. To avoid confusion, they don't contain the `jdbld`
command. Use `../../jdbld` instead.

## Using the projects as examples

The projects that start with `demo-` can also be used as examples for
using the JDrupes Builder. Simply copy the project, remove `_jdbld/test`
and add `../../jdbld` to make the test project a standalone project.
The projects that don't start with `demo-` test edge cases and are therefore
not suitable as examples.
