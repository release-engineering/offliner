---
title: "Plaintext or Comma-Separated Values Files"
---

### Overview

Perhaps the simplest input file type Offliner supports is the plaintext (or comma-separated values) file. This file can be as simple as a list of paths, with no repository base-URLs provided. If you want verification, you can turn it into a comma-separated values file, where the first field is a SHA-256 checksum and the second is the path (again, no repository base-URLs).

### Flexible Checksum Handling

This is Offliner's original file format, and as such we've opted to support the original, bare-bones list-of-paths variation where checksum validation is completely disabled. If your file doesn't provide checksums, the validation step will simply be disabled for your file. If you combine a list-of-paths file with something else that does provide checksums, Offliner will validate checksums for whatever it can, and simply download the rest (and hope for the best).

### Example: List-Of-Paths, The Simplest Possible Offliner Input File

The following illustrates the list-of-paths file that was the original file format for Offliner:

```
/antlr/antlr/2.7.7/antlr-2.7.7.jar
/antlr/antlr/2.7.7/antlr-2.7.7.jar.sha1
```

It is literally just a list of paths to download from the first repository that provides them.

### Example: Comma-Separated Checksums and Paths

This is the other currently supported plaintext format, a comma-separated file with SHA-256 checksum in the first field and path in the second:

```
88fbda4b912596b9f56e8e12e580cc954bacfb51776ecfddd3e18fc1cf56dc4c,/antlr/antlr/2.7.7/antlr-2.7.7.jar
8010ddb1e9e527c9e1ad96931ca339e183092030bbdff0fcb503d92f71c6f785,/antlr/antlr/2.7.7/antlr-2.7.7.jar.sha1
```

This works exactly as the list-of-paths file, except all downloaded files will be verified against their provided checksum.

