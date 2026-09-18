# LBP6030 SFP source map

## New finding

Canon's Linux UFRII LT package for the LBP6030/LBP6030B family contains a source archive for the SFP driver stack. Public Arch packaging metadata for Canon UFRII LT 5.10 shows the source tree is split into:

- `cnrdrvcups-common`: `backend`, `buftool`, `cngplp`, `cnjbig`, `rasterfilter`
- `cnrdrvcups-sfp`: `cngplp`, `cngplp/files`, `cpca`, `StatusMonitor`

The package metadata identifies a mixed licensing model: GPL-2, MIT, and Canon/custom-licensed components. Therefore the source archive is valuable for architecture research, but it is **not automatically safe to copy into USB Print**.

## What matters for our Android port

`rastertosfp` is the CUPS filter that receives CUPS raster and produces the printer-side stream. Public CUPS logs for the LBP6030 show exactly this final filter. The same logs identify the printer as `CA_UFRIILT_OIP` with `CMD:LIPSLX,CPCA`.

The source tree therefore gives us a much better map of the missing layer than the earlier JBIG-only investigation:

`CUPS raster -> rasterfilter/rastertosfp -> CPCA/SFP components -> USB`

## Licensing boundary

Canon's current driver EULA says the main licensed software is proprietary and restricts modification, reverse engineering and derivative works. It separately states that certain modules are provided under GPL and a special exception. The individual source files/modules must therefore be checked before any reuse.

For USB Print, the current rule remains:

- use Canon source only as research material unless the exact module has a compatible redistribution license;
- do not copy proprietary SFP implementation into the Android app;
- do not ship Canon binaries;
- do not enable hardware printing from a guessed reconstruction.

## Next engineering step

Identify the exact source files that implement the LBP6030 SFP byte framing and determine, file-by-file, whether they are GPL/MIT or Canon-restricted. If the complete encoder is not independently redistributable, use the source only to derive externally verifiable behavior and implement a clean-room encoder from that behavior/specification.
