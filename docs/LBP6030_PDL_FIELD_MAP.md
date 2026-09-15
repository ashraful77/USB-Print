# LBP6030 UFR II LT PDL field map

Research-only notes from Canon UFRII LT Linux V5.10 x86-64 disassembly. This file contains no Canon proprietary binaries and does not enable printer transmission.

## Model path

The LBP6030 family PPD is the SFP path and selects `libcanonncapr`. The LBP6030 PPDs define:

```text
CN_PDL_SLC_FI_GN_NL_K: "3,9,6,1,0,0,80"
```

The compression parameter bytes therefore begin:

```text
03 00 09 00 06 01 50 00
```

The `Pdl_StartJob()` code also shows a separate optional calibration branch selected when the parsed `CN_PDL_CTN` value has bit `0x01000000` and `CN_PDL_CV == 5`. This is not required for the ordinary LBP6030 monochrome path and is not evidence that JBIG is used by this model.

## Job-start field mapping

`pdbdlBeginJob(context, job_parameter16)` at `0x15097` constructs:

```text
01 C1 85
<BV:16>
<CV:16>
C2 00
D8 84 <job_parameter16:16>
DD 80 C8
F0 84 08 00
```

For device mode `0x81`, it additionally appends:

```text
E9 84 01 90
```

The two 16-bit values immediately after `C1 85` are `context+0x68` (`CN_PDL_BV`) and `context+0x6c` (`CN_PDL_CV`). These are read directly by `pdbdlBeginJob()`.

The job parameter is the second argument passed by its caller. The function writes the resulting record through `pdWrite()`; it is therefore part of the PDL stream, not an Android/USB transport header.

A previously captured host-only LBP6030 reference begins:

```text
01 C1 85
10 00 10 89
C2 00
D8 84 02 58
DD 80 C8
F0 84 08 00
02
```

The trailing `02` above belongs to the next record in that concatenated host capture, not to `pdbdlBeginJob()` itself.

## Page-start field mapping

`pdbdlBeginPage(context, page_info)` at `0x15593` obtains the normal non-digreg dimensions from:

```text
context+0x1c  -> first dimension
context+0x30  -> second dimension
```

For device mode `0x81`, `context+0x114` overrides the second dimension. The dimensions are passed through `Rotation_Image()` and then emitted as:

```text
03
E7 85 <rotated_dim1:16> <rotated_dim2:16>
DE 80
C8 <page_info+0x24 low byte>
C8 <page_info+0x0c low byte>
CA <page_info+0x18 low byte>
```

The ordinary LBP6030 host reference contains:

```text
03 E7 85
13 60 1B 68
DE 80 00 C8 00 CA A1 00 00 CB 00
```

The first two dimensions therefore resolve to `0x1360 = 4960` and `0x1B68 = 7016` in that capture. This confirms the transfer/page layer uses a 4960-pixel padded width while the Android rasterizer currently represents 4958 active pixels.

## Raster transfer mapping

The `zbdlStartRaster()` call site passes:

```text
arg1 = context
arg2 = encoded line count
arg3 = transfer-width/offset field
arg4 = 0
arg5 = current Y position
arg6 = 3
arg7 = compressed_length + 0x0e (normal band)
       or compressed_length + 0x12 (special final band)
arg8 = buffer returned by slimCompressData()
```

The transfer function writes its header using these numeric fields and then writes the final SLIM buffer. `arg7` is a length, never a pointer.

## End markers

`pdbdlEndPage()` writes a one-byte page-end marker:

```text
13
```

`pdbdlEndJob()` writes a one-byte job-end marker:

```text
11
```

These markers are emitted through `pdWrite()`.

## Current conclusion

The fixed PDL framing around an LBP6030 monochrome page is now substantially mapped. The unresolved part is not the existence of a hidden USB framing protocol: the Canon backend path ultimately sends the PDL byte stream through `pdWrite()` to the output device. The remaining engineering problem is implementing the SLIM compression independently and establishing a complete deterministic reference stream before any physical transmission is enabled.
