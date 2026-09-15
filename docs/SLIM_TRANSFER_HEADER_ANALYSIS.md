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

## 2. `pdbdlTransferHalftoneImage()` exact argument mapping

V5.10 x86-64 symbol: `pdbdlTransferHalftoneImage @ 0x161ab`.

The function uses eight arguments:

```text
arg1 = context
arg2 = 16-bit value from RSI
arg3 = 16-bit value from RDX
arg4 = 16-bit value from RCX
arg5 = 16-bit value from R8
arg6 = low 16 bits of R9
arg7 = transfer value from [RBP+0x10]
arg8 = raster-data pointer from [RBP+0x18]
```

The header begins:

```text
62 E3 85
arg2: 16-bit
arg3: 16-bit
E8 A5
arg4: 16-bit
arg5: 16-bit
E1
arg6: low byte
D7
```

`arg7` is then encoded as either:

```text
84 <arg7:16>
9D <arg7:16>
```

or:

```text
88 <arg7:32>
9E <arg7:32>
```

Transport/context conditions can select `A4 <arg7:16>` or `A8 <arg7:32>`. A context flag can additionally add `E5 <context+0x0b> E4 <context+0x0c>`.

The header is sent with `pdWrite(context, header, header_length)`, followed by the raster-data write.

## 3. Newly verified LBP6030 SLIM call-site arguments

Disassembly of `zbdlStartRaster()` around `0x10d9a-0x10db4` gives the actual call construction:

```text
push [rbp-0x20]       ; arg8: raster-data pointer
push rdi              ; arg7: context+0xa8 value loaded earlier
r9d = 3               ; arg6
r8d = [rbp-0x74]      ; arg5
ecx = 0                ; arg4
rdx = [rbp-0x90]      ; arg3 source
rsi = [rbp-0xe8]      ; arg2
rdi = context         ; arg1
call pdbdlTransferHalftoneImage
```

The callee confirms that `[RBP+0x10]` is the transfer value (`arg7`) and `[RBP+0x18]` is the raster-data pointer (`arg8`). On this SLIM path the fourth register argument is explicitly zero, so `arg4 = 0`.

The important remaining values are therefore:

```text
context[0xa8] -> exact arg7 value
[rbp-0xe8]     -> exact arg2 value
[rbp-0x74]     -> exact arg5 value
[rbp-0x90]     -> exact arg3 value
```

The previously observed host-generated LBP6030 header contains `13 60` and `1B 68` as raster dimensions, consistent with a padded transfer width of `0x1360 = 4960` and height `0x1B68 = 7016`. This is a strong indication that the Canon transfer layer uses a 4960-pixel padded width even though the source 1-bit raster is 4958 pixels wide.

## 4. Consequence for the Android encoder

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

This is only a research scaffold and is **not verified Canon output**. It must remain disabled for physical printing until the context-derived arguments, SLIM raster record, and transport framing are reproduced.

In particular, the Android encoder currently uses raw width `4958` in the raster header; the recovered Canon call-site evidence points toward a padded transfer width of `4960`.

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

The pipeline now has these independently mapped layers:

```text
1-bit raster
  -> lCaptCompEx()
  -> slimCompressData() exact wrapper
  -> pdbdlTransferHalftoneImage() exact call/field construction
  -> pdWrite()
```

The remaining blocker is recovering the exact context values and transport/framing state for the LBP6030B job, then reproducing a complete host-side reference record byte-for-byte.

No physical printer has been used for these experiments.
