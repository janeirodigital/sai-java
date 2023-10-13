# sai-java

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=janeirodigital_sai-java&metric=coverage)](https://sonarcloud.io/summary/new_code?id=janeirodigital_sai-java)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=janeirodigital_sai-java&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=janeirodigital_sai-java)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=janeirodigital_sai-java&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=janeirodigital_sai-java)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=janeirodigital_sai-java&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=janeirodigital_sai-java)

A Java (JDK11+) implementation of the 
[Solid Application Interoperability Specification](https://solid.github.io/data-interoperability-panel/specification/).

Consider participating in the [Solid Interoperability Panel](https://github.com/solid/data-interoperability-panel) if you're
interested in collaborating on or learning more about the evolution of the specification.

Take a look at [CONTRIBUTING](CONTRIBUTING.md) for specifics on how to contribute to this library.

[JavaDocs](https://janeirodigital.github.io/sai-java/) are generated and published with each release.

## Installation

In your `pom.xml`:

```xml
<dependencies>
	<dependency>
		<groupId>com.janeirodigital</groupId>
		<artifactId>sai-java</artifactId>
		<version>preferred.version.here</version>
	</dependency>
</dependencies>
```

## Getting Started

This library implements all of the patterns defined in the 
[Solid Application Interoperability Specification](https://solid.github.io/data-interoperability-panel/specification/),
which will hereafter be referred to as "the specification".
Consequently, becoming familiar with that text and the patterns therein will be helpful, and we will link to the
pertinent ones throughout.

### Initializing the Application

The first thing your application will need to do is initialize an `Application` instance. An application instance
in this context represents an [Application](https://solid.github.io/data-interoperability-panel/specification/) 
as defined in the specification.

```java
URI applicationId = URI.create("https://projectron.example/id");
AuthorizedSessionAccessor sessionAccessor = new BasicAuthorizedSessionAccessor();
boolean validateSsl = true;
boolean validateShapeTrees = false;
boolean refreshTokens = true;
Application app = new Application(applicationId, validateSsl, validateShapeTrees, refreshTokens, sessionAccessor);
```

Lets break down the parameters provided above:

* `applicationId`: This is the URI of the Application's Profile ID. Effectively, the WebID of the Application.

* `sessionAccessor`: This is an implementation of the 
[`AuthorizedSessionAccessor`](https://janeirodigital.github.io/sai-authentication-java/com/janeirodigital/sai/authentication/AuthorizedSessionAccessor.html) 
interface as defined by 
[sai-authentication-java](https://github.com/janeirodigital/sai-authentication-java).
It lets the application get and put authorized session credentials used to interface
with a given Social Agent's data on their behalf. A basic in-memory implementation is 
provided along with that library, but production
scenarios may want to provide a more robust implementation with something like Redis
or even an RDBMS.

* `validateSsl`: Toggle whether to skip SSL validation and errors. **Only for use in development or test scenarios**.

* `validateShapeTrees`: Toggle whether to enable client-side shape tree validation. Useful when storage servers
do not apply server-side validation. When false, no validation will be applied. This validation is performed
by intercepting requests sent and received via OkHttpClient instances.

* `refreshTokens`: Toggle whether to refresh tokens automatically when they expire.

