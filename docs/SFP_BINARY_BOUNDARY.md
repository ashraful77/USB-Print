# SFP / CPCA binary boundary

## 2026-09 investigation result

The Canon UFRII LT 5.00 packaging for the LBP6030 family gives us a useful distinction between the buildable source tree and the printer-stream runtime.

Public packaging metadata shows these common modules being built from source:

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

The same package is explicitly mixed-license, with module-specific license files including a Canon license for `cpca/cnpklib`. Therefore a source directory name alone is not enough to establish that its implementation can be copied into USB Print.

## Critical finding

The Canon 5.00 binary package installs a separate executable/filter named:

`rastertosfp`

The LBP6030 PPD declares:

`application/vnd.cups-raster -> rastertosfp`

and

`application/vnd.cups-command -> rastertosfp`

Public Linux logs independently confirm `rastertosfp` as the final CUPS filter before the USB backend. The available packaging evidence also shows that the filter is accompanied by Canon-specific runtime libraries.

The current evidence therefore supports:

`PDF -> 1-bit raster -> SFP/CPCA encoding -> USB`

with `rastertosfp` as the unresolved stream-emission boundary.

## Do not substitute CAPT

Open-source CAPT implementations were found, but public device lists classify LBP6030/LBP6030B/LBP6030w as UFR-II. CAPT implementations target different Canon protocol families. Therefore CAPT framing is not being treated as a drop-in replacement.

This is important because a working open-source protocol for another Canon printer is useful research, but it is not evidence that those bytes are accepted by the LBP6030B.

## What we can safely use as research

We can use public source trees, PPDs, package metadata, logs, module documentation, and externally observable behavior to determine:

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

1. Trace the public CPCA source tree and module licenses.
2. Determine whether any independently redistributable CPCA framing implementation exists.
3. Separate generic compression from Canon-specific framing.
4. Build deterministic vectors only from independently sourced behavior.
5. Keep the Android encoder fail-closed until those vectors are available.

No Canon binary or guessed printer bytes are being added to the application.
