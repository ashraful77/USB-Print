# UFR II LT implementation plan

## Verified facts

- Canon identifies the LBP6030/LBP6030B/LBP6030w as UFR II LT.
- Canon's Linux UFRII LT V5.10 supports Linux ARM.
- PDF bytes and generic packed raster bytes are not valid printer jobs.

## Rules

Do not send guessed headers, raw PDF, or raw raster data to the printer.
Do not bundle Canon proprietary binaries.
Do not treat reverse-engineered Canon binary behavior as a redistributable implementation.

## Current milestone

1. PDF is rendered to 1-bit monochrome raster data.
2. USB printer communication is isolated.
3. Ufr2Encoder is an explicit fail-closed boundary.
4. Next task: audit public UFR II/SFP source and licensing and identify the smallest independently redistributable encoder/filter component that can be ported to Android ARM64.

## Research lead

The public ondrej-zary/carps-cups UFR-II JBIG work is a research lead, not yet a complete LBP6030B encoder.