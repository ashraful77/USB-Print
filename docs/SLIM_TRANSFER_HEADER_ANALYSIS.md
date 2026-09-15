# Canon SLIM Transfer Header Analysis

Research-only reverse-engineering notes for Canon UFR II LT V5.10 and the LBP6030 family. No Canon proprietary binary is added to this repository.

## 1. `slimCompressData()` exact wrapper

V5.10 x86-64 symbol: `slimCompressData @ 0x14da9`.

Observed arguments:

```text
rdi  context
rsi  compression parameter pointer
rdx  output destination
rcx  compressed input length
r8   compressed input pointer
r9d  trailer flag
```

The function writes:

```text
COMPPARAM[0..5]
mode byte
32-bit native-endian value = compressed_length + 4
compressed bytes
```

On little-endian x86-64 the length is little-endian. The mode byte is `0` when the trailer flag is non-zero, otherwise `1`.

When the trailer flag is non-zero, it additionally writes:

```text
BD 3C BC [BC or DC] 80
```

The fourth trailer byte is `BC` when `(context + 0x64) & 0x04` is set, otherwise `DC`.

## 2. `pdbdlTransferHalftoneImage()` header construction

V5.10 x86-64 symbol: `pdbdlTransferHalftoneImage @ 0x161ab`.

The function clears a local 36-byte buffer and constructs a variable-length header beginning:

```text
62 E3 85
```

The first fields are:

```text
62 E3 85
arg1: 16-bit
arg2: 16-bit
E8 A5
arg3: 16-bit
arg4: 16-bit
E1
arg5: low byte
D7
```

`arg6` is then encoded in either 16-bit or 32-bit form:

```text
84 <arg6:16>
9D <arg6:16>
```

or:

```text
88 <arg6:32>
9E <arg6:32>
```

Transport/context conditions can select the shorter variants:

```text
A4 <arg6:16>
A8 <arg6:32>
```

Another context flag can add:

```text
E5 <context+0x0b> E4 <context+0x0c>
```

The completed header is sent through `pdWrite()` before the raster payload.

## 3. Important call-site result

The SLIM raster path in `zbdlStartRaster()` calls `pdbdlTransferHalftoneImage()` around `0x10daf` with:

```text
arg3 = 0
arg5 = 3
```

A separate transfer path around `0x10366` uses:

```text
arg3 = 0
arg5 = 5
```

Therefore the raster header is context-dependent; it is not safe to assume that width, line count and compressed length alone determine every field.

## 4. Consequence for `ufr2engine.cpp`

The current Android implementation uses a guessed structure resembling the real Canon function:

```text
62 E3 85
W
lines
E8 A5
W
lines
E1 00
D7
84 payload_length
9D payload_length
```

This is useful as a research scaffold but is **not verified Canon output**. It must remain disabled for physical printing until the six transfer arguments and surrounding job/raster context are recovered.

## 5. Verified LBP6030 monochrome compression parameters

The target PPD supplies:

```text
CN_PDL_SLC_FI_GN_NL_K: "3,9,6,1,0,0,80"
```

The corresponding Canon parameter structure begins:

```text
03 00 09 00 06 01 50 00
```

A direct host-side `lCaptCompEx()` test using this parameter set produced deterministic compressed output, including an 808-byte result for a blank 256-line band.

## 6. Current research boundary

The pipeline now has two independently mapped layers:

```text
1-bit raster
  -> lCaptCompEx()
  -> slimCompressData() exact wrapper
  -> pdbdlTransferHalftoneImage() header construction
  -> pdWrite()
```

The remaining blocker is recovering the exact raster-context arguments and transport/framing state for the LBP6030B job. No physical printer has been used for these experiments.
