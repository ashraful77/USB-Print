# SFP source / binary boundary

## 2026-09 investigation result

The Canon UFRII LT 5.00 packaging for the LBP6030 family gives us a useful distinction between the **buildable source tree** and the **printer-stream runtime**.

Public packaging metadata for the Canon source archive shows these common modules being built from source:

- `backend`
- `buftool`
- `cngplp`
- `cnjbig`
- `rasterfilter`

The SFP-specific source tree builds:

- `cngplp/files`
- `cngplp`
- `cpca`
- `StatusMonitor`

The same package is explicitly marked as mixed-license: GPL-2, MIT, and custom Canon terms. The packaging scripts also preserve module-specific license files, including a Canon license for `backend` and `cpca/cnpklib`. Therefore a source directory name alone is not enough to establish that its implementation can be copied into USB Print.

## Critical finding

The Canon 5.00 binary package installs a separate executable/filter named:

`rastertosfp`

The LBP6030 PPD declares:

`application/vnd.cups-raster -> rastertosfp`

and

`application/vnd.cups-command -> rastertosfp`

This means the actual CUPS-to-printer stream boundary is the `rastertosfp` filter. Public packaging metadata places that filter in the binary driver package rather than proving that the Android-usable implementation is contained in the redistributable source modules.

The current evidence therefore supports this architecture:

`PDF -> 1-bit raster -> SFP/CPCA encoding -> USB`

with `rastertosfp` as the missing implementation boundary.

## What we can safely use as research

We can use public source trees, PPDs, package metadata, logs, and module documentation to determine:

- filter names and data-flow order;
- page/raster parameters;
- model identification;
- USB/backend separation;
- module dependencies;
- externally observable behavior;
- licensing boundaries.

We must not copy Canon custom/proprietary implementation into the Android project merely because it is available in a driver archive.

## Android gate

The encoder remains fail-closed until one of these is established:

1. an independently redistributable implementation of the required SFP/CPCA stream is found; or
2. a clean-room implementation can be derived from sufficiently public specifications and independently obtained behavioral test vectors.

A structurally plausible stream is not sufficient. Hardware transmission should remain disabled until a deterministic encoder passes offline validation and then produces verified output on the target LBP6030B.

## Next experiment

The next useful experiment is **not** to guess protocol bytes.

Instead:

1. obtain the exact Canon LBP6030 driver package/source in a legally usable research environment;
2. inspect the module-specific README/license files;
3. trace the build/link relationship between `rasterfilter`, `cpca`, and `rastertosfp`;
4. identify which component actually emits the printer stream;
5. record only externally observable format facts;
6. search for an independently licensed implementation of those facts;
7. add deterministic golden vectors before enabling USB output.

## Current conclusion

The research has moved the blocker from “unknown UFR II protocol” to a much narrower question:

**Can the LBP6030 SFP stream encoder be implemented from independently usable material, or is the required byte-generation logic confined to Canon-restricted runtime code?**

Until that question is answered, USB Print should keep `Ufr2Encoder` fail-closed.
