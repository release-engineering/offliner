---
title: "Maven Metadata Generation"
---

### Overview

Once Offliner completes all downloads listed in the manifest files, it will generate Maven metadata files for the content in the target directory. This is important for some Maven builds, especially those that use:

* plugins with no version declaration
* dependencies with version ranges

Without Maven metadata files, project builds with any of the above will fail. Therefore, basic version metadata is essential for any offline-capable Maven repository.

### Metadata Generation Process 

In order to generate Maven metadata files for the target repository directory, Offliner performs the following operations:

1. Scan the target directory structure looking for any file ending with `.pom`
2. Parse each matching path to extract a Maven groupId (G), artifactId (A), and version (V)
3. Collect a list of versions (V) for each unique groupId/artifactId (GA) combination
4. For each GA, generate a `maven-metadata.xml` file in the corresponding directory that contains a sorted list of the versions discovered for that GA during the scan

### Gotchas

Currently, Offliner doesn't generate Maven metadata for the following:

* Plugin prefixes
* Artifact SNAPSHOT directories

