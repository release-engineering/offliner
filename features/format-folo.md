---
title: "Indy's Folo Tracking Record Format (JSON)"
---

### Overview

If you're using [Indy](https://commonjava.github.io/indy) to track the content used (and produced) by your builds, you can use those same tracking reports to produce an offline-capable Maven repository for that build by using Offliner. If you have a very complex product (or project) involving many builds, and you need an offline repository for that, you can merge these files or just feed a whole set of them into Offliner for download. Or you could write a bash / python script to iterate over a directory of tracking records and download them all into some target repository directory.

The point is that Indy can produce these records automatically as it proxies content into your builds, then you can reuse them *unchanged* with Offliner to produce an offline-capable Maven repository.

### Example Tracking Record

Consider the following tracking record snippet:

```
{
  "key" : {
    "id" : "offliner-20160707"
  },
  "uploads" : [ {
    "storeKey" : "hosted:local-deployments",
    "path" : "/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/maven-metadata.xml",
    "localUrl" : "https://repo.my.co/api/hosted/local-deployments/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/maven-metadata.xml",
    "md5" : "cd4ce328e076ae1c5d324576e3b047c9",
    "sha256" : "05c8472598fbd3069efe7f91ca3099e6c4b787f9c5f40f1b627aaba02cb51e6c"
  }, {
    "storeKey" : "hosted:local-deployments",
    "path" : "/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/maven-metadata.xml.md5",
    "localUrl" : "https://repo.my.co/api/hosted/local-deployments/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/maven-metadata.xml.md5",
    "md5" : "5de9d655b8df2303f6e8dd3e2d04b4b1",
    "sha256" : "cbfa0c784b67c71e0e90c410533e0593fa487c0ba02f481c751044e373bf0a62"
  }, {
    "storeKey" : "hosted:local-deployments",
    "path" : "/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/offliner-1.1-20160707.165129-1-javadoc.jar",
    "localUrl" : "https://repo.my.co/api/hosted/local-deployments/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/offliner-1.1-20160707.165129-1-javadoc.jar",
    "md5" : "ff636d80c9827934d09771bc5b6122aa",
    "sha256" : "f4236cb4fdd713bc44b8357c0bb5b55b81a5641b932e4d5f43643cab415a18b6"
  }, {
    "storeKey" : "hosted:local-deployments",
    "path" : "/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/offliner-1.1-20160707.165129-1-javadoc.jar.md5",
    "localUrl" : "https://repo.my.co/api/hosted/local-deployments/com/redhat/red/offliner/offliner/1.1-SNAPSHOT/offliner-1.1-20160707.165129-1-javadoc.jar.md5",
    "md5" : "66d759ebf9a8ed6f575b567ddd5c6856",
    "sha256" : "8cbc66e09dcabcbda61f6b28b668f7a51b9f695de4c636981c470ac6af94c07b"
  } ],
  "downloads" : [ {
    "storeKey" : "remote:central",
    "path" : "/antlr/antlr/2.7.7/antlr-2.7.7.jar",
    "originUrl" : "http://repo1.maven.apache.org/maven2/antlr/antlr/2.7.7/antlr-2.7.7.jar",
    "localUrl" : "https://repo.my.co/api/remote/central/antlr/antlr/2.7.7/antlr-2.7.7.jar",
    "md5" : "f8f1352c52a4c6a500b597596501fc64",
    "sha256" : "88fbda4b912596b9f56e8e12e580cc954bacfb51776ecfddd3e18fc1cf56dc4c"
  }, {
    "storeKey" : "remote:central",
    "path" : "/antlr/antlr/2.7.7/antlr-2.7.7.jar.sha1",
    "originUrl" : "http://repo1.maven.apache.org/maven2/antlr/antlr/2.7.7/antlr-2.7.7.jar.sha1",
    "localUrl" : "https://repo.my.co/api/remote/central/antlr/antlr/2.7.7/antlr-2.7.7.jar.sha1",
    "md5" : "b0c9d87f8bc6d71f33915cb559402533",
    "sha256" : "8010ddb1e9e527c9e1ad96931ca339e183092030bbdff0fcb503d92f71c6f785"
  },
```

(The full sample is [here](samples/folo-tracking-record.json).)

This record is fairly straightforward. It contains a small header that gives the tracking / session key ('offliner-20160707') which I selected when I setup my Maven `settings.xml`, and two additional sections, each one of which contains entries for each tracked file.

#### Anatomy of a Tracked Content Entry

It is worth describing the structure of these tracking entries in a bit more detail. The following fields are available:

* `storeKey` - The Indy repository key from which the file was retrieved (or stored, in the case of an upload). It consists of `type:name` where type is one of `remote`, `hosted`, or more rarely, `group`.
* `path` - The path within the repository which was retrieved (or stored)
* `originUrl` - Only available if the file was proxied from some upstream (remote) server, this is the original URL used to download the file into Indy's cache. This URL will only appear if the type section in the `storeKey` is `remote`.
* `localUrl` - The URL for directly accessing this file from Indy. It does **not** contain the Folo tracking / session id, so accessing the file via this URL will not change tracking records at all. If the file is hosted locally in the Indy instance (type sub-field in the `storeKey` is `hosted`), this will be the only URL available for the file.
* `md5` - The MD5 hash for the file.
* `sha256` - The less collision-prone SHA-256 hash for the file.


#### Upload Entries...NOT PROCESSED

The first non-header section in the record is for file uploads. These generally correspond to Maven artifact deployment actions, though anything capable of a HTTP PUT could add an entry to this list. This usually describes the output of a build.

The uploads list is ignored by Offliner. Working from the assumption that Offliner is trying to support you in *building* a project, we've decided the output of someone else's build of that project isn't really useful to you. So it's not included. It may be appropriate to revisit this decision at some point in the future, especially since uploads wouldn't be that hard to include.


#### Download Entries

The last section in the record is for downloaded files. This list is comprehensive, requiring absolutely no guesswork or interpolation on the part of Offliner. If the build downloaded a file, and it went through Indy tagged with the given session id, then it's in the tracking record.

For each of these entries, Offliner will attempt to retrieve the `originUrl`. If that's missing, it will use the `localUrl`. Once it has settled on a URL for the file, it trims the `path` from that URL to arrive at a base-URL for a repository to use when downloading files. This base-URL is added to the repository list for Offliner to use, and the `path` (along with its associated `sha256`) is added to the list of files to download.

### No Maven Settings Here

It's also worth noting that the artifact-list handler for Folo tracking records does **not** process a Maven `settings.xml`, so the `--mavensettings` command-line option has no effect here.

