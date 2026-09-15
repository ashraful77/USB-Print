# Canon V5.10 Encoder Boundary

## Status

This document records the verified research boundary for the Canon LBP6030/LBP6030B UFR II LT encoder. It is intentionally non-operational: no Canon proprietary binary is copied into the Android build, and the Android encoder must not emit guessed printer bytes.

Canon's V5.10 Linux driver officially lists the LBP6030/LBP6030B/LBP6030w and Linux ARM among its supported models/platforms.

## Verified pipeline

```text
CUPS 1-bit raster
    -> Pdl_RasterData
    -> zbdlStartRaster
    -> lCaptCompEx
    -> slimCompressData
    -> pdbdlTransferHalftoneImage
    -> pdWrite / cnpkSendData
    -> Canon transport/backend
```

For the monochrome 600-DPI LBP6030 path, the research vector uses:

- Raster width: 4958 pixels
- Normal band size: 256 lines
- Raster depth: 1 bit
- Bytes per line: 620
- `copyMin`: 2
- `LPLONG encodedLineCount`: supplied by Canon
- `unused`: NULL

The `COMPPARAM` structure is 8 bytes and is populated from Canon job parameters. The observed monochrome parameter string is:

```text
3,9,6,1,0,0,80
```

which maps to:

```text
03 00 09 00 06 01 50 00
```

## Important correction to the previous Android implementation

The current experimental `app/src/main/cpp/ufr2engine.cpp` uses a synthetic 2-bit conversion and guessed PDL/CMLP framing. That is **not** the Canon SLIM path and must not be treated as a verified LBP6030B encoder.

The real Canon path compresses the original 1-bit raster with `lCaptCompEx()` before constructing the SFP raster record.

## Reference vector

A host-side call to Canon's SLIM implementation with a blank 4958x256 1-bit band produced:

```text
encoded lines: 256
compressed length: 808 bytes
```

The beginning of that reference output was:

```text
BE FD BC 88 AC D5 BF FD BA 7C 88 AC D0 BF FD BA
7C 88 AC D0 BF FD BA 7C 88 AC D0 BF FD BA 7C 88
AC D0 BF FD BA 7C 88 3F DC A6 FD 0C B1 9C 64 BA
2C D0 BF F4 8A BD 18 A7 BC 6E B1 3C D5 BA 7C 88
```

This is a **reference observation only**, not a protocol specification and not an independently implemented encoder.

## Raster-record boundary

Canon's `pdbdlTransferHalftoneImage()` builds a local raster command/header and then writes the compressed raster separately. The header is constructed dynamically from raster/context values; it must not be replaced with a fixed guessed 36-byte record.

The downstream `pdWrite()` path reaches Canon's `cnpkSendData()` implementation. Large payloads are internally handled in chunks, so the Linux driver's internal transport buffering must not automatically be equated with the Android USB/CMLP framing currently present in the experimental app.

## Safety gate

Before enabling the Android encoder for physical printing, the project requires all of the following:

1. Independent, legally usable implementation or separately licensed encoder component.
2. Deterministic golden vectors for blank, vertical-line, horizontal-line, checkerboard, and text-like raster bands.
3. Exact raster-record construction validated against host-side reference output.
4. Verified job/page/end framing.
5. Malformed-input and boundary tests.
6. Code review confirming that no guessed bytes can reach the physical USB printer.
7. One fixed monochrome hardware test page only after the above gates pass.

Until those conditions are met, the Android native encoder remains research-only and should fail closed rather than send an unverified stream.

## Licensing boundary

Canon's driver contains proprietary components. This repository must not copy or redistribute Canon proprietary binaries merely to make the Android app work. Any future implementation must use independently authored code or source/binaries whose license explicitly permits the intended use.
