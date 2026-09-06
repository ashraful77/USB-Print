# SFP Encoder Next Gate

## Current evidence

Canon's current UFRII LT V5.10 Linux driver officially supports the LBP6030/LBP6030B/LBP6030w family and lists Linux ARM support. The older V5.00 package explicitly supports the same family and uses CUPS.

The V5.00 package identifies the downloadable archive as:

`linux-UFRIILT-drv-v500-uken-18.tar.gz`

Public build investigation identifies the source archive inside the package as:

`Sources/cnrdrvcups-sfp-5.00-1.tar.gz`

and the extracted source tree as `cnrdrvcups-sfp-5.00`, with `allgen.sh` as a build entry point.

The LBP6030 PPD routes CUPS raster and CUPS command input to `rastertosfp`.

## New investigation findings

Public build recipes confirm that the SFP source tree is not a self-contained Android-ready encoder. The V5.00 build is split between `cnrdrvcups-common-5.00` and `cnrdrvcups-sfp-5.00`. The SFP tree contains `cngplp`, `cpca`, and `StatusMonitor`; the common tree contains components including `backend`, `buftool`, `cngplp`, `cnjbig`, and `rasterfilter`.

This means the next engineering task is dependency tracing around the actual `rastertosfp` path rather than copying the entire Canon driver.

The Canon common-module project documents `buftool` as a byte-order-independent buffer library and states that its `buftool` and `libcnpk.so` modules are MIT licensed, while `cngplp` is GPL; it also warns that licensing can vary by file. Exact source-file headers must therefore be audited before reuse.

A public build report also shows that the source build can require `buftool`, and that the packaged `rastertosfp` binary is a separate runtime filter. This is evidence that the encoder path has internal dependencies that must be isolated rather than guessed.

Independent NXP investigation of the same printer family confirms the real CUPS pipeline:

`application/vnd.cups-raster -> rastertosfp -> printer/canon -> USB backend`

and identifies the printer as `Canon LBP6030/6040/6018L`, with device ID fields including `CID:CA_UFRIILT_OIP` and `CMD:LIPSLX,CPCA`.

These observations strengthen the architecture but do **not** constitute a verified UFR II LT wire-format specification. They must not be used to generate experimental printer bytes.

## Licensing boundary

Canon's current V5.10 license page explicitly distinguishes Canon/licensor software from separately licensed third-party modules. It grants GPL rights to specified modules but otherwise restricts redistribution and derivative works of Canon software. Therefore USB-Print must not copy Canon's proprietary driver or proprietary binaries into the APK. Any reused source must be individually identified, license-cleared, and kept within the applicable license terms.

## Encoder investigation plan

1. Obtain the official V5.00 source archive for analysis.
2. Extract only for analysis; do not commit the archive or proprietary binaries.
3. Extract `Sources/cnrdrvcups-sfp-5.00-1.tar.gz`.
4. Trace `rastertosfp` build targets from `allgen.sh` and subordinate makefiles.
5. Build a dependency graph ending at the raster-to-SFP conversion path.
6. Separate UI/status-monitor/CUPS plumbing from the actual raster encoding path.
7. Identify compression and bit-packing code used by the print path.
8. Audit copyright/license headers for every candidate source file.
9. Determine whether the minimum encoder can be independently rebuilt for Android ARM without CUPS/GTK.
10. Create deterministic golden vectors before enabling physical printing.

## Physical printer safety

No experimental or guessed byte stream may be sent to the physical Canon printer.

The first permitted hardware test remains one controlled fixed one-page test pattern. PDF printing is a later stage.

## Current Android status

The application remains fail-closed. `SfpEncoder` is the protocol boundary and `UnavailableSfpEncoder` deliberately generates no bytes. USB transfer is only allowed after a verified encoder returns a complete encoded SFP job.

## Evidence threshold for enabling the encoder

The encoder must satisfy all of the following before `isAvailable` becomes true:

- deterministic output for fixed raster input;
- repeatable output across builds/runs;
- validated page/job metadata;
- known output framing and transfer expectations;
- component-level license clearance;
- unit tests covering malformed input and golden vectors;
- code review of the physical-test path;
- controlled one-page hardware validation on the target LBP6030 family.
