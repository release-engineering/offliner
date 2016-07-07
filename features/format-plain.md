---
title: "Plaintext or Comma-Separated Values Files"
---

### Overview

Perhaps the simplest input file type Offliner supports is the plaintext (or comma-separated values) file. This file can be as simple as a list of paths, with no repository base-URLs provided. If you want verification, you can turn it into a comma-separated values file, where the first field is a SHA-256 checksum and the second is the path (again, no repository base-URLs).

### Flexible Checksum Handling

This is Offliner's original file format, and as such we've opted to support the original, bare-bones list-of-paths variation where checksum validation is completely disabled. If your file doesn't provide checksums, the validation step will simply be disabled for your file. If you combine a list-of-paths file with something else that does provide checksums, Offliner will validate checksums for whatever it can, and simply download the rest (and hope for the best).
 
