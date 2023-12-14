[![License: LGPL v3](https://img.shields.io/badge/License-LGPL_v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)
[![Test ADQLLib](https://github.com/gmantele/vollt/actions/workflows/adqllib.yml/badge.svg)](https://github.com/gmantele/vollt/actions/workflows/adqllib.yml)

# Preamble

This project aims to provide a Java library to parse, manipulate and translate
ADQL queries. It is called _ADQLLib_ and is part of the VOLLT library-set.

# ADQL

ADQL stands for Astronomical Data Query Language. It is a language developed by
the [IVOA](http://www.ivoa.net/ "International Virtual Observatory Alliance").
The standard describing this language is available at
<https://www.ivoa.net/documents/ADQL/>.

This library currently supports the following versions of this language:
- [v2.1](https://www.ivoa.net/Documents/ADQL/20231107/index.html)
- [v2.0](https://www.ivoa.net/Documents/cover/ADQL-20081030.html)

# About this library

- **Java version:** 8 or more
- **ADQL grammar language:** [JavaCC](https://javacc.github.io/javacc/)
- **License:** [LGPL v3 license](https://www.gnu.org/licenses/lgpl-3.0)
  (details in [COPYING](COPYING) and [COPYING.LESSER](COPYING.LESSER))


- **Supported SQL translations:** PostgreSQL, PgSphere, MySQL, SQLServer.
- [Online documentation](http://cdsportal.u-strasbg.fr/adqltuto/)
- [Online parser/validator](http://cdsportal.u-strasbg.fr/adqltuto/validator.html)

# Usage examples

In addition to the online documentation and the Javadoc, one may find simple
usage examples in the directory [examples/](examples) of this repository.

For the moment, only examples on how to parse an ADQL query are available:

- [Parse a query](examples/adql/example/parse/A_SimpleQueryParsing.java)
- [Parse an ADQL clause](examples/adql/example/parse/B_SimpleClauseParsing.java)
- [Handle parse errors](examples/adql/example/parse/C_HandleParseException.java)
- [Declare an optional features](examples/adql/example/parse/D_DeclareOptionalFeatures.java)
- [Declare a UDF](examples/adql/example/parse/E_DeclareUDF.java)

# Build the library

This project uses [Gradle](https://gradle.org/) as build automation tool. It
is configured for this project in [build.gradle](build.gradle). All dependencies
declared in this configuration are automatically resolved. It also includes
configuration for Eclipse and IntelliJ IDEA ; for Visual Studio Code one may
install an extension such as [Gradle for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle).

To use Gradle, one may install it on its own computer (see
[installation instructions](https://gradle.org/install/) in the Gradle website).
Alternatively, one may simply use the provided [gradlew](../gradlew) script
(or [gradlew.bat](../gradlew.bat) for Windows) to run exactly the same build
commands. This latter will download Gradle in the temporary hidden directory
dedicated to this project [.gradle/](../.gradle). This way, you are always
guaranteed to use exactly the same version of Gradle used by the source code
developer.

Here are the main available commands:

_**Note:** To simplify, the Gradle binary command used below is `gradle`, but it
can be substituted by `../gradlew` or `../gradlew.bat` depending on your
configuration._

- Generate final products:
  - JAR file: `gradle jar`
  - Runnable JAR file: `gradle runnableJar`
  - Distribution ZIP/TAR (JAR + all dependencies): `gradle distZip`
    _(or `gradle distTar`)_
  - Documentation (Javadoc): `gradle javadoc`

- Build classes:
  - All classes and ADQL parsers: `gradle build`
  - Only the ADQL parsers (JavaCC): `gradle compileJavacc`

- Run tests (Junit): `gradle test`

- Remove all generated files: `gradle clean`

- List all available Gradle tasks: `gradle tasks`

# Contribution

I strongly encourage you **to declare any issue you encounter**
[here](https://github.com/gmantele/taplib/issues). Thus, anybody who has the
same problem can see whether his/her problem is already known. If the problem is
known, the progress and/or comments about its resolution will be published.

In addition, if you have forked this repository and made some corrections on
your side which are likely to interest any other user of the libraries, please,
consider **sending a pull request**
[here](https://github.com/gmantele/taplib/pulls). If these modifications are
compatible with the IVOA definition and are not too specific to your usecase,
they will be integrated (maybe after some modifications) on this repository and
thus made available to everyone potentially interested too.