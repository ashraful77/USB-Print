# LBP6030 Raster Transfer Call-Site — 2026-09-15

Research-only reverse-engineering note. No Canon proprietary binary is added to this repository.

## Corrected `pdbdlTransferHalftoneImage()` call mapping

The x86-64 `zbdlStartRaster()` call site around `0x10d70-0x10db4` resolves the two stack arguments as follows:

```text
10d70: edx = [rbp-0xb4]        ; compressed result length
10d76: eax = [rbp-0x50]        ; transfer-length adjustment
10d79: edi = edx + eax         ; arg7
10d7c: eax = [rbp-0x74]        ; current raster Y
10d7f: ecx = sign_extend(eax:16)
10d82: rax = [rbp-0xe8]        ; encoded line count
10d89: edx = zero_extend(rax:16)
10d8c: rax = [rbp-0x90]        ; transfer width/offset
10d93: esi = zero_extend(rax:16)
10d96: rax = [rbp-0x8]         ; context
10d9a: push [rbp-0x20]         ; arg8 = compressed buffer
10d9d: push rdi               ; arg7 = compressed length + adjustment
...
10da4: r8d = ecx              ; arg5 = current Y
10da7: ecx = 0                 ; arg4
10daf: call pdbdlTransferHalftoneImage
```

Therefore the earlier interpretation that `arg7` was a compressed-buffer pointer was incorrect.

## Verified argument values

For the normal monochrome/non-digreg path:

```text
arg2 = encoded line count returned by lCaptCompEx()
arg3 = context+0x1c, low 16 bits
arg4 = 0
arg5 = context+0x24, low 16 bits
arg6 = 3
arg7 = compressed_length + transfer_adjustment
arg8 = context-local compressed output buffer
```

`transfer_adjustment` is loaded from `[rbp-0x50]` and is normally `0x0e` (14 bytes). A last-band special path sets it to `0x12` (18 bytes) and also sets `[rbp-0x4c] = 1` for the trailer variant of `slimCompressData()`.

Thus, for a normal band:

```text
arg7 = compressed_length + 14
```

and for the special final-band path:

```text
arg7 = compressed_length + 18
```

The callee's final data write uses `arg8` with `arg7` as the byte count, matching this call-site construction.

## Raster geometry

The same call path establishes:

```text
source raster width: 4958 pixels
transfer width:      4960 pixels
A4 height @ 600 dpi: 7016 lines
```

The host-generated transfer header contained:

```text
13 60 1B 68
```

which is consistent with `4960 × 7016`.

The current Y position is advanced by the encoded line count after a successful transfer, so band sequencing is stateful.

## Compression chain

The recovered sequence is:

```text
1-bit raster band
  -> lCaptCompEx(input, output, lineBytes, lineCount, capacity,
                 bitsPerPixel, &encodedLines, &compParam, 2, NULL)
  -> slimCompressData(context, compParam, finalBuffer,
                      compressedLength, compressedBuffer, trailerFlag)
  -> pdbdlTransferHalftoneImage(context,
                                encodedLines, transferWidth, 0,
                                currentY, 3,
                                compressedLength + adjustment,
                                finalBuffer)
  -> pdWrite(header)
  -> pdWrite(finalBuffer, transferLength)
```

For LBP6030 monochrome the PPD supplies:

```text
CN_PDL_SLC_FI_GN_NL_K = "3,9,6,1,0,0,80"
```

with the corresponding initial compression parameter bytes:

```text
03 00 09 00 06 01 50 00
```

## Safety status

This resolves a significant ambiguity in the transfer layer, but it is **not yet a complete printable UFR II LT implementation**. USB transport framing, job/page state, context initialization, and the exact raster record still need host-side verification.

The Android encoder must remain fail-closed. No guessed bytes should be sent to the physical LBP6030B until a deterministic host reference record has been reconstructed and validated.
