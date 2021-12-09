# Contributing to sai-java

Thanks for your interest in sai-java. Feedback, questions, issues, and 
contributions are both welcomed and encouraged.

## Getting Started

A thorough understanding of the [specification](https://solid.github.io/data-interoperability-panel/specification/),
implemented by this library is essential to any substantive contributions.

Note that for those interested in contributing to the 
[Solid Application Interoperability Specification](https://solid.github.io/data-interoperability-panel/specification/),
please consider participating in the [Solid Interoperability Panel](https://github.com/solid/data-interoperability-panel/).

## Contributions

Contributions to sai-java should be made in the form of [pull requests](https://github.com/xformativ/sai-java/pulls). Each pull request
will be reviewed by one or more core contributors.

## Releases

Releases are performed by the 
[Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/) as part
of Github Actions. They must be triggered manually via the
[Publish Release Workflow](https://github.com/xformativ/sai-java/actions/workflows/maven-release.yml).

1. Choose `Run workflow`
1. Adjust settings for the maven release
    * Use workflow from: `Brain: main`
    * Minor version increment: `true` if a minor version increment (1.0.0 -> 1.1.0) is desired
    * Major version increment: `true` if a major version increment (1.0.0 -> 2.0.0) is desired
1. Adjust settings for the Github release
    * Is this a draft (not finalized) release? `true` if the github release should be saved in draft form
    * Is this a prerelease? `true` if this is not meant to be a production ready public release
    * Release summary: Textual summary of the release
1. Click the green `Run workflow` button

This will result in:

* Git tag created for the release
* Github release created off of that tag
* Artifacts pushed to specified repositories 
* Version numbers bumped in the pom.xml(s) and set to SNAPSHOT