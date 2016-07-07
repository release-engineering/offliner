---
title: "Checksums and Download Avoidance"
---

### Overview

Currently, two of the three Offliner manifest-file input formats (plaintext and Folo) allow you to specify a checksum for each path. If provided, these checksums will be used to verify the target repository content.

**TIP: Offliner only works with SHA-256 checksums.**

In addition, Offliner respects the content that already exists in the target repository directory. If a path listed in the manifest file already exists in the target directory, Offliner will try to use it instead of re-downloading.

### Checksum Verification

Offliner verifies checksums in two ways: on pre-existing files, and during the download streaming process for the files it retrieves.

#### Existing File Verification

If Offliner encounters a pre-existing file that has a corresponding entry in the manifest, and if that entry also contains a checksum, Offliner will attempt to verify the existing file's checksum against the the manifest. If the existing file doesn't match, Offliner will delete the file to make room for a fresh download.

#### Download Stream Verification

Offliner calculates checksums for each of the files it downloads. If the corresponding manifest entry specifies a checksum, Offliner will compare the one in the manifest to the one calculated on download. If they don't match, Offliner reports an error to the user. Downloads that fail checksum verification are not preserved in the target directory.
