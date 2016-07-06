---
title: "Maven POM Input Format"
---

### Overview

If you build with Apache Maven, you're already familiar with this file format. The Maven POM has many sections and configuration options, but Offliner will only use two: `<repositories/>` and `<dependencies/>`. Simply create a Maven POM file containing repository entries for any additional repository URLs you wish to include in the download (https://repo.maven.apache.org/maven2 and https://maven.repository.redhat.com/ga are included by default). Then, add a dependency section containing the artifacts you wish to have downloaded. Keep in mind that for every artifact listed, Offliner will also attempt to download the corresponding `.pom` file.

### Example POM

Here's a basic example of a POM that Offliner could read for a list of downloadable paths:

```
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.myproj</groupId>
  <artifactId>my-project</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>

  <repositories>
    <repository>
      <id>myco-repo</id>
      <url>http://repo.my.co/maven</url>
      <snapshots><enabled>false</enabled></snapshots>
      <releases><enabled>true</enabled></releases>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>commons-io</groupId>
      <artifactId>commons-io</artifactId>
      <version>2.4</version>
    </dependency>
    <dependency>
      <groupId>commons-io</groupId>
      <artifactId>commons-io</artifactId>
      <version>2.4</version>
      <type>jar.sha1</type>
    </dependency>
    <dependency>
      <groupId>commons-io</groupId>
      <artifactId>commons-io</artifactId>
      <version>2.4</version>
      <type>pom.sha1</type>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>2.3.1</version>
      </plugin>
    </plugins>
  </build>
</project>
```

There are four basic parts to the example above. First is the POM header. This is information that makes this a valid Maven POM (usable by Maven). While there may be some parts of this you could leave out without affecting Offliner's use of the file, it's generally safer to make Maven POMs valid whenever you can.

#### Repositories in the POM

Second is the repositories section. This is a listing of Maven repositories to be used when accessing content listed in the dependencies. While not 100% technically correct from a Maven perspective, Offliner will also use repositories declared in this section to download plugin files (see below). The URLs given in this section can be remapped when used in conjunction with a Maven settings.xml (again, see below). When declared, these will augment the default list of repositories that Offliner uses (Maven central and Red Hat's product repository).

#### Dependencies

The third section above defines a series of dependencies. Offliner translates these into a series of file paths to be downloaded, and will attempt to mix and match these paths with each repository base-URL until either a download succeeds for that path, or it runs out of repository base-URLs. It's a bit of a brute-force approach, but it has the advantage of being simple and predictable. Notice that not only does the POM list the `jar` dependency you're used to seeing, but it also lists dependencies on `jar.sha1` and `pom.sha1` typed artifacts for the same project version (`commons-io:commons-io:2.4`). When Offliner encounters a dependency, it automatically adds a second path to download that dependency's associated POM file. However, it doesn't download checksum files by default. If your Maven configuration is strict about checksum files, you'll have to add them as separate dependencies as above.

#### Plugins

Finally, this example POM lists a single plugin, `maven-compiler-plugin`, in the main plugins section. Offliner also scans the plugins section of the POM, and downloads `jar` and `pom` files for each plugin it encounters. 

#### What Offliner Does Not Do

Currently, Offliner does **not** look at plugin-level dependency declarations, dependencyManagement sections, or pluginManagement sections. It won't process dependencies or plugins declared in profiles. Offliner is intentionally simplistic in its understanding of Maven POMs. It will **not** recursively process the POMs for dependencies and plugins in your Maven POMs; this introduces a level of complexity that Offliner specifically resists.

To do otherwise would reduce the stability of the tool, and lead to ambiguous behavior.

#### More Information on Maven POMs

For more information on the Maven POM format, see: [https://maven.apache.org/pom.html](https://maven.apache.org/pom.html).

### Maven Settings

If you want, you can use a Maven `settings.xml` file to provide authentication credentials or redirect the repository URLs declared in a POM. 

Consider the following sample `settings.xml` file:

```
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>my-repo-manager</id>
      <url>https://maven.internal.my.co/repo</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>

  <servers>
    <server>
      <id>my-repo-manager</id>
      <username>someuser</username>
      <password>mypassword</password>
    </server>
  </servers>
</settings>
```

#### URL Redirection

URL redirection (`<mirrors/>` in the `settings.xml`) can be useful if you're using a POM produced by someone else, and you have your own Maven repository manager such as Sonatype Nexus, or JFrog's Artifactory (or [Commonjava Indy](https://commonjava.github.io/indy)) with proxy repositories setup for the repositories declared in the POM. Using such an approach can make your download more resilient to network connectivity problems, and can even be used to pre-populate your repository manager's cache. This is similar to using Offliner's `--repo-url` command-line option.

In the example above, there are three elements to consider: `id`, `url`, and `mirrorOf`. The `id` provides a name which can be used to lookup authentication configuration (again from the `settings.xml`, see below). The `url` provides the alternative URL which should be used for any matching repositories (instead of the one they declare). Finally, the `mirrorOf` element determines which repositories to redirect. The value of the `mirrorOf` element are comma-delimited, and can include a `*` wildcard or a list of repository `id`'s.

#### Credentials

If one or more repositories - either declared in the POM or redirected via the `settings.xml` - requires authentication, you need a mechanism for declaring your credentials. In the Maven `settings.xml`, this is the `servers` section. An example looks like this:

As you can see, the example above specifies a mirror that redirects all declared repositories from the POMs you specify, then provides a username and password for accessing the repository manager that is your redirection target. The server section's `id` field matches that of the mirror, which tells Offliner that these credentials should be used for connections to the given mirror.

Any repositories, whether they're declared in the POM or in the `settings.xml` as mirrors, can have credentials attached using the same mechanism. Just specify a server entry with an `id` that matches the repository's `id`, and Offliner will use the credentials with any connection to that repository. 

#### More Information on Maven Settings

For more information on the Maven `settings.xml` format, see: [https://maven.apache.org/settings.html](https://maven.apache.org/settings.html). But remember, only the `mirrors` and `servers` sections are used by Offliner.


