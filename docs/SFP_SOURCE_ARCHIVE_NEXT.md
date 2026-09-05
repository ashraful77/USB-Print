# SFP source archive: next gate

## Confirmed

The Canon UFRII LT v5.00 Linux bundle contains a nested source archive:

`Sources/cnrdrvcups-sfp-5.00-1.tar.gz`

The extracted tree is reported as `cnrdrvcups-sfp-5.00` and includes build scripts and multiple submodules. Public packaging and PPD evidence identify `rastertosfp` as the filter used for the LBP6030 family.

## Android decision

Do not copy Canon binaries into the APK and do not guess SFP protocol bytes.

The next gate is source inspection: identify which files actually implement raster-to-SFP encoding, map their licenses and dependencies, and determine whether an independently redistributable subset can be built for ARM64 Android without CUPS/Ghostscript.

Until that gate is passed and validated against a real LBP6030B, the print engine remains fail-closed.

## Hardware target

Canon imageCLASS LBP6030B, USB VID:PID `04A9:2795`, UFR II LT, 600 dpi.
