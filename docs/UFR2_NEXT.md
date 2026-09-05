# UFR II LT implementation plan

## Verified facts

- The Canon LBP6030 family uses UFR II LT.
- The printer must receive a Canon UFR II LT job; PDF bytes and generic packed raster bytes are not valid printer jobs.
- Canon's Linux driver is CUPS-based and includes model-specific UFR II components.
- The public `vicamo/cndrvcups-lb` project contains GPL-licensed Canon UFR II driver source and a `pstoufr2cpca` CUPS filter, which is useful for protocol/source investigation.

## Android implementation rule

Do not send guessed headers, raw PDF, or raw raster data to the printer. Do not bundle Canon proprietary binaries without appropriate redistribution rights.

The Android implementation should isolate the encoder behind `Ufr2Encoder`. A verified compatible open-source implementation can later replace the current safe stub without changing PDF rendering or USB transport.

## Current milestone

1. PDF is rendered to 1-bit monochrome raster data.
2. USB printer-class communication is verified.
3. `Ufr2Encoder` is now an explicit safety boundary and returns no printer data until a compatible encoder is implemented.
4. Next engineering task: inspect the GPL-compatible UFR II source and determine the smallest portable encoder/filter component that can be built for Android/ARM64.
