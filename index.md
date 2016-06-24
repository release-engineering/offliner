---
---

### Overview

Offliner is a very simple multi-file downloader, currently designed to support downloading Maven repositories for use in building a particular project or set of projects. 

Offliner accepts various types of manifest files as input, then constructs lists of URLs from these files and downloads them into a single target directory. Depending on the manifest format, it can also do checksum verification as the files are downloaded, or verification of pre-existing files in the target directory (if they're listed in the manifest). After all downloads are complete, Offliner will scan the download target directory and generate Maven repository metadata files from the paths that are present.

The end result of an Offliner execution is a directory structure you can import into your own Maven repository manager (or even just park behind your static HTTP server), and use directly from a Maven build.

### What Offliner Is (And What It Is Not)

One of the main goals of Offliner is to be as stable as possible, avoiding the need for frequent application updates. For this reason, though it does support the Maven POM format as an input file, it does not support crawling through a graph of POM files as Maven would. This tends to be a complex process filled with edge cases and calling for quite a bit of encoded assumptions about how to resolve ambiguous situations. In order to provide the most stable application possible, Offliner avoids all of this complexity, expecting instead that the person generating the manifest file will do the work to produce a flat list of files to download.

As a result, Offliner isn't much more sophisticated than a bash script with an embedded for-loop that uses wget. It does avoid re-downloading pre-existing files in the target directory, checksum verification, and Maven metadata generation. It can accept multiple input files in a single execution, allowing you to download repository content for multiple sets of projects at once. And, since it can avoid pre-existing files in the target directory, Offliner supports building up your target repository directory over successive invocations.

While Offliner is intended to be used to produce repositories which can be hosted on internal infrastructure separated from the Internet by an air gap, it downloads whatever content is listed in the manifest file. This means you could theoretically download content to support a fully offline build, or some specific subset of that content. It's really up to the manifest author to decide.

### Download and Use. It's That Simple.

You can obtain the jar for the latest release [here](http://repo.maven.apache.org/maven2/com/redhat/red/offliner/offliner). Once downloaded, Offliner can be executed very simply:

    $ java -jar offliner-<version>.jar [OPTIONS] FILE...

Offliner supports the following arguments:

  * **-P (--proxy-pass) PASS** - Password for authenticating to a proxy
  * **-U (--proxy-user) USER** - User for authenticating to a proxy
  * **-c (--connections) INT** - Number of concurrent connections to allow for downloads *(default: 200)*
  * **-d (--download, --dir) DIR** - Download directory *(default: ./repository)*
  * **-h (--help)** - Print this help screen and exit
  * **-m (--maventypemapping) MAPPING** - File containing mapping properties where key is type and value is file extension with or without classifier each mapping on a single line. List elements are separated by semicolons.
  * **-p (--password, --repo-pass) PASS** - Authentication password, if using a repository manager URL
  * **-r (--url, --repo-url, --base-url) REPO-URL** - Alternative URL for resolving repository artifacts (eg. repository manager URL for proxy of maven.repository.redhat.com)
  * **-s (--mavensettings) FILE** - Path to settings.xml used when a pom is used as the source file
  * **-u (--user, --repo-user) USER** - Authentication user, if using a repository manager URL
  * **-x (--proxy) HOST[:PORT]** - Proxy host and port (optional) to use for downloads

