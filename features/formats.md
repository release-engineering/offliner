---
---

### Overview

Offliner currently supports three manifest-file formats:

* Maven POM (`pom.xml` or `*.pom`)
* Indy Folo Tracking Report JSON (`*.json`)
* Plaintext or Comma-Separated Values (any other file)

### Format Details

#### Maven POMs

If you build with Apache Maven, you're already familiar with this file format. The Maven POM has many sections and configuration options, but Offliner will only use two: `<repositories/>` and `<dependencies/>`. Simply create a Maven POM file containing repository entries for any additional repository URLs you wish to include in the download (https://repo.maven.apache.org/maven2 and https://maven.repository.redhat.com/ga are included by default). Then, add a dependency section containing the artifacts you wish to have downloaded. Keep in mind that for every artifact listed, Offliner will also attempt to download the corresponding `.pom` file.

As an example, let's look at a POM for downloading the artifacts necessary to build Offliner itself using an offline repository. First, I'll use a helper script[1] to run the Offliner build and generate a POM from the resulting local repository:

```
    $ ./generate-offliner-pom.py offliner/

#### Indy Folo Tracking Reports

**TODO.**

#### Plaintext or Comma-Separated Values

**TODO.**

