README
======

Preambule
---------

This GitHub repository contains the sources of the library-set named VOLLT. It
contains 3 libraries implementing [IVOA](http://www.ivoa.net/ "International Virtual Observatory Alliance")
standards and protocols:

* [ADQL-2.1](http://www.ivoa.net/documents/ADQL/20180112/index.html "Astronomical Data Query Language")
* [UWS-1.1](http://www.ivoa.net/documents/UWS/20161024/index.html "Universal Worker Service pattern")
* [TAP](http://www.ivoa.net/documents/TAP/ "Table Access Protocol")

_**NOTE:** Support of ADQL-2.1 currently under development. For the moment, TAP
is still using ADQL-2.0 by default._

### Documentation
For a complete documentation/tutorial and a demo of the 3 libraries you should visit the following websites: [ADQLTuto](http://cdsportal.u-strasbg.fr/adqltuto), [UWSTuto](http://cdsportal.u-strasbg.fr/uwstuto) and [TAPTuto](http://cdsportal.u-strasbg.fr/taptuto).

### Java version
These libraries are developed in **Java 7**.

### License
The three of these libraries are under the terms of the [LGPL v3 license](https://www.gnu.org/licenses/lgpl.html). You can find the full description and all the conditions of use in the files src/COPYING and src/COPYING.LESSER.

Collaboration
-------------

I strongly encourage you **to declare any issue you encounter** [here](https://github.com/gmantele/taplib/issues). Thus anybody who has the same problem can see whether his/her problem is already known. If the problem is known the progress and/or comments about its resolution will be published.

In addition, if you have forked this repository and made some corrections on your side which are likely to interest any other user of the libraries, please, **send a pull request** [here](https://github.com/gmantele/taplib/pulls). If these modifications are in adequation with the IVOA definition and are not too specific to your usecase, they will be integrated (maybe after some modifications) on this repository and thus made available to everybody.

Repository content
------------------

### Libraries
Each library has its own package (`adql` for ADQL, `uws` for UWS and `tap` for TAP). These packages are independent except `tap` which needs the two other packages. In addition to these packages, you will also find `cds` which is a dependency for the libraries.

### Dependencies
Below are summed up the dependencies of each library:

|                        | ADQL | UWS | TAP |
|------------------------|:----:|:---:|:---:|
| Package `adql`         |  X   |     |  X  |
| Package `cds.utils`    |  X   |     |  X  |
| Postgres JDBC Driver   |  X   |     |  X  |
| Package `uws`          |      |  X  |  X  |
| JSON library           |      |  X  |  X  |
| HTTP Servlet API       |      |  X  |  X  |
| HTTP Multipart Library |      |  X  |  X  |
| Packages `cds.*`       |      |     |  X  |
| STIL Library           |      |     |  X  |

In the `lib` directory, you will find 3 JAR files:
* The *HTTP Multipart Library*: `commons-fileupload-1.3.3.jar` (and `commons-io-2.6.jar`). This library helps dealing with uploads.
* The *[STIL Library](http://www.star.bris.ac.uk/~mbt/stil/)*: `stil_3.3-2.jar` (i.e. packages `nom.tap`, `org.apache.tools.bzip2`, `uk.ac.starlink`). This library helps supporting VOTable (read and write) and some other output formats.
* The *JSON Library*: `json-20180813.jar` (i.e. the former included package `org.json`). This library helps manipulating JSON content. _This library was already used (before v4.4 of UWS-Lib and v2.3 of TAP-Lib) but it was included in the sources instead of being considered as an external library._

The *Postgres JDBC Driver* is needed ONLY IF you want to use (and keep) `adql.translator.PgSphereTranslator`. You can get this driver on the [PostgreSQL website](https://jdbc.postgresql.org/download.html). The required package for the ADQL and TAP libraries is `org.postgresql` (and particularly the class `org.postgresql.Driver`).

The *HTTP Servlet API* is generally available in the libraries coming along the Web Application Server you are using. For instance, for Tomcat, it is in the directory `lib` (or `/var/lib/tomcat-x/lib` if installed with Aptitude on a Linux system ; `x` is the version number of Tomcat). The required package for the UWS and TAP library is `javax.servlet`.

*__Note:__ The Postgres JDBC Driver and the HTTP Servlet API are not provided in this Git repository in order to avoid version incompatibility with the host system (i.e. your machine when you checkout/clone/fork this repository).*

### JUnit

The sources of these three libraries come with some JUnit test files. You can find them in the `test` directory.

If you are using Eclipse (or maybe also with another Integrated Development Environment), JUnit is generally already available. Then you can directly execute and compile the provided JUnit test files. So you do not need the two libraries mentionned just below.

Otherwise, you will need to get the JUnit library. Generally it is provided with the JDK, but you can find the corresponding JAR also on the [JUnit website](https://github.com/junit-team/junit4/wiki/Download-and-Install).

You may also need another library called `hamcrest`. You can find this one on its [Maven repository](http://search.maven.org/#search|ga|1|g%3Aorg.hamcrest) ; just to be sure to have everything needed, just take `hamcrest-all` as a JAR.

*__Note:__ The JUnit and Hamcrest libraries are not provided in this Git repository in order to avoid version incompatibility with the host system (i.e. your machine when you checkout/clone/fork this repository).*

### ANT scripts
At the root of the repository, there are 3 ANT scripts. Each is dedicated to one library. They are able to generate JAR for sources, binaries and Javadoc.

4 properties must be set before using one of these scripts:
* `POSTGRES` *only for ADQL and TAP if you want to keep adql.translator.PgSphereTranslator*: a path toward a JAR or a binary directory containing all `org.postgresql.*` - [https://jdbc.postgresql.org/download.html](JDBC Postgres driver)
* `SERVLET-API` *only for UWS and TAP*: a path toward a JAR or a binary directory containing all `javax.servlet.*`
* `JUNIT-API` *not required if you are not interested by running the JUnit tests*: a path toward one or several JARs or binary directories containing all classes to use JUnit.
* `JNDI-API`  *only for TAP AND only if you are interested by running the JUnit tests*: a path toward one or several JARs or binary directories containing all classes to run a JNDI. Several libraries exist for that ; [Simple-JNDI](https://code.google.com/archive/p/osjava/wikis/SimpleJNDI.wiki) is very simple and is used by the libraries developer to run the related JUnit tests.

*__Note:__ No JNDI library is provided in this Git repository because any JNDI Library may work and there is no reason to impose a specific one. Besides, similarly as the other libraries required to compile the sources, it lets avoiding version incompatibility with the host system (i.e. your machine when you checkout/clone/fork this repository).*

All of these ANT scripts have the following main targets:
* `junitValidation`: Executes all JUnit tests related to the target library and stop ANT at any error. If the target library is TAP, the JUnit tests of the three libraries are run.
* `buildLib` *DEFAULT*: run the JUnit tests and if they are all successful, compile the target library's classes and build a JAR file with them and their dependencies.
* `buildLibAndSrc`: same as `buildLib` + building of a JAR file containing all the sources and the required libraries.
* `buildJavadoc`: generate a JAR containing the Javadoc of the target library's classes.
* `buildAll`: equivalent of `buildLibAndSrc` and `buildJavadoc` together. The result is 3 JARs: one with the compiled classes, one with the corresponding sources and the last one with the Javadoc.

### Gradle build
The code can be built with Gradle, either as a jar file to be included in other projects 
or as a war file to be deployed in Tomcat.
