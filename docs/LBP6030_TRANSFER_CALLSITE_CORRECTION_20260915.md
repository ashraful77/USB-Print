# LBP6030 Transfer Call-Site Correction — 2026-09-15

Research-only note. No Canon proprietary binary is included.

## Corrected `arg7` interpretation

The earlier research note incorrectly described the seventh argument as if its value were loaded directly from `context+0xa8`. The latest `zbdlStartRaster()` call-site disassembly resolves this.

The caller does:

```text
edx = [rbp-0xb4]        ; compressed raster result length
eax = [rbp-0x50]        ; transfer adjustment
edi = edx + eax         ; seventh argument
```

Then it pushes the compressed output pointer separately as the eighth argument:

```text
push [rbp-0x20]         ; arg8 = compressed output buffer
push rdi                ; arg7 = compressed length + adjustment
```

Therefore:

```text
normal band:       arg7 = compressed_length + 0x0e
special last band: arg7 = compressed_length + 0x12
arg8:              compressed output buffer
```

The `pdbdlTransferHalftoneImage()` callee uses arg7 as the transfer length for its data write, confirming that it is a length/value rather than a pointer.

## Recovered raster arguments

For the normal monochrome path:

```text
arg2 = encoded line count
arg3 = context+0x1c (low 16 bits)
arg4 = 0
arg5 = context+0x24 (low 16 bits)
arg6 = 3
```

The Y-position field is advanced by the number of encoded lines after each successful transfer.

For the LBP6030B 600-DPI A4 configuration, the observed transfer width is `0x1360 = 4960` pixels while the PDF raster width is `4958` pixels. This indicates two padded pixels in the transfer representation.

Page height is `0x1B68 = 7016` lines.

## Consequence

The Android encoder must **not** substitute a guessed payload length or pointer into arg7. The next reconstruction target is the exact construction of the compressed-data record and the surrounding job/page/transport writes.

No physical printer test should be performed until those layers are deterministic.
