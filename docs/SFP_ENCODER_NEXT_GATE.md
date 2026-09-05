# SFP Encoder Next Gate

## Current evidence

Canon's UFRII LT V5.00 package explicitly supports the LBP6030/LBP6030B/LBP6030w family and uses CUPS. The package identifies the downloadable archive as `linux-UFRIILT-drv-v500-uken-18.tar.gz`.

Public build investigation identifies the source archive inside the package as:

`Sources/cnrdrvcups-sfp-5.00-1.tar.gz`

and the extracted source tree as `cnrdrvcups-sfp-5.00`, with `allgen.sh` as a build entry point.

The LBP6030 PPD routes CUPS raster and CUPS command input to `rastertosfp`.

## New conclusion

The next task is **source acquisition and dependency tracing**, not protocol guessing.

The Canon package license notice distinguishes Canon/licensor software from separately licensed free-software components. Therefore USB-Print must not copy the Canon package wholesale into the APK. Any reused component must first be individually identified and checked for redistribution compatibility.

## Encoder investigation plan

1. Obtain the official V5.00 archive.
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
