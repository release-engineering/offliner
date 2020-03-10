# Offliner Maven Repository Downloader

This is a simple tool that consumes lists of artifacts in a few formats, then downloads them to a local directory structure. It can verify checksums if those are available on the upstream server, and it can repair / generate metadata files for the artifacts it downloaded. It's able to consume multiple artifact lists in a single execution, or run multiple times with the same target location and merge the resulting content + metadata.

For more information, see [https://release-engineering.github.io/offliner].
