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

Contributions to sai-java should be made in the form of Github pull requests. Each pull request
will be reviewed by one or more core contributors.

## Releases

### Ensure Github Credentials are Setup

Your Maven settings in `$HOME/.m2/settings.xml` will need to be updated with an entry for github that includes a 
[personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).

It will look something like:

```shell
<servers>
  <server>
      <id>github</id>
      <username>your-github-username<username>
      <password>your-long-password-token</password>
  </server>
</servers>
```

### Run Maven Release Process

Releases are performed by the 
[Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/).

1. Perform a dry-run to catch any issues before any operations that change the project are executed.
   
```shell
mvn release:prepare -DdryRun=true
```

2. Be sure to cleanup after any dry-run before doing a proper release.

```shell
mvn release:clean
```

3. Prepare a proper release

```shell
mvn release:prepare
```

*Note that the above will run in interactive mode. Use `--batch-mode` for non-interactive processing.*