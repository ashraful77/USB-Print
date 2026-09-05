# SFP / UFR II LT Encoder Source Audit

## Target

The target printer family is Canon LBP6030/6040/6018L, including the LBP6030B variant. The Android print path must ultimately transform the normalized monochrome raster into a complete SFP/UFR II LT printer job before USB transfer.

## Confirmed upstream structure

Canon's Linux UFRII LT V5.00 package is publicly listed as `linux-UFRIILT-drv-v500-uken-18.tar.gz` and explicitly supports LBP6030/LBP6030B/LBP6030w. The package uses CUPS.

The documented source layout contains:

- `Sources/cnrdrvcups-sfp-5.00-1.tar.gz`
- extracted `cnrdrvcups-sfp-5.00`
- an `allgen.sh` build entry point
- a companion `cnrdrvcups-common-5.00` tree

The LBP6030 PPD routes both `application/vnd.cups-raster` and `application/vnd.cups-command` to `rastertosfp`.

## Dependency warning

The SFP package is not currently suitable for blind inclusion in the Android APK. Community package metadata reports dependencies including CUPS, GTK-related libraries, libxml2, cairo/pango, JPEG and JBIG libraries. The Android implementation therefore needs a component-level audit rather than copying the Linux binary or the entire source tree.

## Required audit

Before implementing the encoder, inspect the V5.00 source archive and identify:

1. the exact translation path from CUPS raster to SFP/UFR II LT;
2. all source files linked into `rastertosfp`;
3. all compression/bit-packing routines used by the target path;
4. whether each required source file has a redistributable license compatible with this project;
5. whether Canon-specific proprietary components are required at runtime;
6. whether the required subset can be rebuilt for Android ARM without CUPS/GTK;
7. deterministic input/output vectors for a one-page fixed test pattern.

## Safety gate

No guessed SFP/UFR II LT byte stream is permitted to reach a physical printer. The current application intentionally remains fail-closed until an encoder produces deterministic, independently validated output.

The first physical test must be a controlled one-page fixed test print. PDF printing comes only after that test succeeds.

## Current Android boundary

`SfpEncoder` is the application-level boundary. Its implementation must return a complete printer job rather than PDF or generic raster data. `UnavailableSfpEncoder` currently emits no bytes, which is intentional.

The USB transport accepts only encoded SFP job data. Raster normalization and job validation remain separate from protocol encoding.

## Current status

The source location and `rastertosfp` pipeline are established, but the actual encoder implementation has **not** yet been proven. Do not enable printing merely because the source archive exists or because the Linux filter can be located.

## Next gate

Obtain and inspect the actual Canon V5.00 source archive. Extract the SFP source tree and build graph. Then implement only the minimum legally usable and technically sufficient subset, with deterministic golden vectors and unit tests before any physical USB test.
