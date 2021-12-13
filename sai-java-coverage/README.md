# sai-java-coverage

This module generates an accurate JaCoCo code coverage report across all of the modules
of sai-java. JaCoCo is designed to provide coverage reports on individual modules, each of which contains 
tests and the code being tested.

The most reliable way to generate one comprehensive test coverage output is for a seperate module (this one)
to generate an aggregate report. The [sai-java-coverage/pom.xml](./pom.xml) does exactly this. It includes
the other modules as dependencies, automatically including them in the aggregate report that is generated
by the maven `test` goal (e.g. run `mvn clean test` on the command line), and stored in 
`target/site/jacoco-aggregate`.

See [this explanation from JaCoCo](https://github.com/jacoco/jacoco/wiki/MavenMultiModule) on the rationale
for this approach.
