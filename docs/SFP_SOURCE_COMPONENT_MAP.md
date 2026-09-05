# SFP source component map

## Evidence

Public packaging of Canon UFRII LT v5.00 extracts the nested source archive:

`Sources/cnrdrvcups-sfp-5.00-1.tar.gz`

The package build separates the source into:

- `cnrdrvcups-common-5.00`
- `cnrdrvcups-sfp-5.00`

The common tree is built from `backend`, `buftool`, `cngplp`, `cnjbig`, and `rasterfilter`.

The SFP tree is built from `cngplp/files`, `cngplp`, `cpca`, and `StatusMonitor`.

The packaged runtime filter is `rastertosfp`, and the LBP6030 family PPD routes both CUPS raster and CUPS command input through that filter.

## Important limitation

The public packaging recipe does **not** establish that any one of these source directories is a self-contained implementation of the SFP byte stream. It also declares mixed licensing (`GPL2`, `MIT`, and `custom`) and external dependencies such as CUPS, libxml2/libglade, GTK, and JBIG.

Therefore this map is an investigation aid, not an Android integration approval.

## Android gate

Before adding encoder code to USB-Print, identify the exact source files that produce `rastertosfp` output and inspect their individual license headers and dependencies. Only a clearly redistributable, self-contained subset may be considered for ARM64 Android.

Do not copy Canon binaries into the APK, and do not infer protocol bytes from the PPD alone.

The print engine remains fail-closed until a deterministic encoded-vector test and physical LBP6030B validation are available.
