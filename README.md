# JDrupes Builder

[![Build status](https://github.com/mnlipp/JDrupes-Builder/actions/workflows/jekyll.yml/badge.svg)](https://github.com/mnlipp/JDrupes-Builder/actions/workflows/jekyll.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/56aae350ec2b465f84e2ca22d1208003)](https://app.codacy.com/gh/mnlipp/JDrupes-Builder/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

JDrupes Builder (jdbld) is a
[build automation tool](https://en.wikipedia.org/wiki/Build_system_(software_development))
that uses Java code for its configuration and models builds as collections
of resources that are produced on demand.
See the project [website](https://builder.jdrupes.org) for more information.

## Building

The projects itself uses its own `jdbld` configuration for build operations.
This implies a bootstrap problem which can be solved in two ways. Either
run the builder from Eclipse or use the previous (published) version of the
builder. While the former approach should always work, the latter approach
may fail if there are incompatible changes in the API.
