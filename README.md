[![Test ADQLLib](https://github.com/gmantele/vollt/actions/workflows/adqllib.yml/badge.svg)](https://github.com/gmantele/vollt/actions/workflows/adqllib.yml)
[![Test UWSLib](https://github.com/gmantele/vollt/actions/workflows/uwslib.yml/badge.svg)](https://github.com/gmantele/vollt/actions/workflows/uwslib.yml)
[![Test TAPLib](https://github.com/gmantele/vollt/actions/workflows/taplib.yml/badge.svg)](https://github.com/gmantele/vollt/actions/workflows/taplib.yml)

# Preamble

This GitHub repository contains the sources of the library-set named VOLLT. It
contains 3 libraries implementing [IVOA](http://www.ivoa.net/ "International Virtual Observatory Alliance")
standards and protocols:

* [ADQL-2.1](http://www.ivoa.net/documents/ADQL/20180112/index.html "Astronomical Data Query Language")
* [UWS-1.1](http://www.ivoa.net/documents/UWS/20161024/index.html "Universal Worker Service pattern")
* [TAP](http://www.ivoa.net/documents/TAP/ "Table Access Protocol")

# About these libraries

- **Java version:** 8 or more
- **License:** [LGPL v3 license](https://www.gnu.org/licenses/lgpl.html)


- Online documentations:
  - [for ADQLLib](http://cdsportal.u-strasbg.fr/adqltuto/)
  - [for UWSLib](http://cdsportal.u-strasbg.fr/uwstuto/)
  - [for TAPLib](http://cdsportal.u-strasbg.fr/taptuto/)

# Build the library

This project uses [Gradle](https://gradle.org/) as build automation tool. It
is configured for each project in its `build.gradle`. All dependencies
declared in this configuration are automatically resolved. It also includes
configuration for Eclipse and IntelliJ IDEA ; for Visual Studio Code one may
install an extension such as [Gradle for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle).

To use Gradle, one may install it on its own computer (see
[installation instructions](https://gradle.org/install/) in the Gradle website).
Alternatively, one may simply use the provided [gradlew](gradlew) script
(or [gradlew.bat](gradlew.bat) for Windows) to run exactly the same build
commands. This latter will download Gradle in the temporary hidden directory
dedicated to this project [.gradle/](.gradle). This way, you are always
guaranteed to use exactly the same version of Gradle used by the source code
developer.

Gradle commands can be applied to a single project or to all of them:

- For a single project, two possibilities:
  1. Go inside the project directory and merely run the command
     (ex: `cd ADQLLib && gradle jar`).
  2. Prefix the command to run with the project name
     (ex: `gradle :ADQLLib:jar`).

- For all projects: just run the command on the root level

Here are the main available commands for all libraries:

_**Note:** To simplify, the Gradle binary command used below is `gradle`, but it
can be substituted by `../gradlew` or `../gradlew.bat` depending on your
configuration._

- Generate final products:
    - JAR file: `gradle jar`
    - Runnable JAR file: `gradle runnableJar`
    - Distribution ZIP/TAR (JAR + all dependencies): `gradle distZip`
      _(or `gradle distTar`)_
    - Documentation (Javadoc): `gradle javadoc`

- Build classes: `gradle build`

- Run tests (Junit): `gradle test`

- Remove all generated files: `gradle clean`

- List projects: `gradle -q projects`

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
