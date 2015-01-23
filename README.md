README
======

Preambule
---------

This GitHub repository contains the sources of 3 libraries implementing [IVOA](http://www.ivoa.net/ "International Virtual Observatory Alliance") standards and protocols:
* [ADQL](http://www.ivoa.net/documents/latest/ADQL.html "Astronomical Data Query Language")
* [UWS](http://www.ivoa.net/documents/UWS/index.html "Universal Worker Service pattern")
* [TAP](http://www.ivoa.net/documents/TAP/ "Table Access Protocol")

### Documentation
For a complete documentation/tutorial and a demo of the 3 libraries you should visit the following websites: [ADQLTuto](http://cdsportal.u-strasbg.fr/adqltuto), [UWSTuto](http://cdsportal.u-strasbg.fr/uwstuto) and [TAPTuto](http://cdsportal.u-strasbg.fr/taptuto).

### Java version
These libraries are developed in **Java 1.6**.

### License
The three of these libraries are under the terms of the [LGPL v3 license](https://www.gnu.org/licenses/lgpl.html). You can find the full description and all the conditions of use in the files src/COPYING and src/COPYING.LESSER.

Collaboration
-------------

I strongly encourage you **to declare any issue you encounter** [here](https://github.com/gmantele/taplib/issues). Thus anybody who has the same problem can see whether his/her problem is already known. If the problem is known the progress and/or comments about its resolution will be published.

In addition, if you have forked this repository and made some corrections on your side which are likely to interest any other user of the libraries, please, **send a pull request** [here](https://github.com/gmantele/taplib/pulls). If these modifications are in adequation with the IVOA definition and are not too specific to your usecase, they will be integrated (maybe after some modifications) on this repository and thus made available to everybody.

Repository content
------------------

### Libraries
Each library has its own package (`adql` for ADQL, `uws` for UWS and `tap` for TAP). These packages are independent except `tap` which needs the two other packages. In addition to these packages, you will also find `cds` and `org.json` which are dependencies for the libraries.

### Dependencies
Below are summed up the dependencies of each library:
* ADQL: `adql`, `cds.utils`, `org.postgresql` *(for adql.translator.PgSphereTranslator only)*
* UWS: `uws`, `org.json`, HTTP Multipart lib. (`com.oreilly.servlet`)
* TAP: `adql`, `uws`, `cds.*`, `org.json`, `org.postgresql` *(for adql.translator.PgSphereTranslator only)*, HTTP Multipart lib. (`com.oreilly.servlet`), STIL (`nom.tap`, `org.apache.tools.bzip2`, `uk.ac.starlink`)

In the `lib` directory, you will find 2 JAR files:
* `cos-1.5beta.jar` to deal with HTTP multipart requests
* `stil3.0-5.jar` for [STIL](http://www.star.bris.ac.uk/~mbt/stil/) (VOTable and other formats support)

### ANT scripts
At the root of the repository, there are 3 ANT scripts. Each is dedicated to one library. They are able to generate JAR for sources, binaries and Javadoc.

3 properties must be set before using one of these scripts:
* `POSTGRES`: a path toward a JAR or a binary directory containing all org.postgresql.* - [https://jdbc.postgresql.org/download.html](JDBC Postgres driver) - **(ONLY for ADQL and TAP if you want to keep adql.translator.PgSphereTranslator)**
* `SERVLET-API`: a path toward a JAR or a binary directory containing all javax.servlet.*
* (`JUNIT-API` *not required before the version 2.0 of the tap library OR if you are not interested by the `test` directory (JUnit tests)*: a path toward one or several JARs or binary directories containing all classes to use JUnit.)
