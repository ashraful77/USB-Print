# Canon `pdWrite()` / transport analysis

Research-only notes for Canon UFR II LT V5.10 and the LBP6030 family. No Canon proprietary binary is added to this repository.

## `pdWrite()`

V5.10 x86-64 `pdWrite @ 0x14315` is a very thin wrapper:

```text
pdWrite(context, buffer, length)
    -> cnpkSendData(context->output, buffer, length)
```

It returns success when `cnpkSendData()` succeeds. There is no PDL-header transformation inside `pdWrite()` itself.

## `cnpkSendData()` direct-output path

V5.10 x86-64 `cnpkSendData @ 0x32ba0` has two broad modes selected by the output context's first word.

When that mode word is zero, the function enters the direct-output path. It obtains a file descriptor from the output context and repeatedly calls the native `write(fd, buffer, remaining)` until all requested bytes have been written.

Therefore, in the direct-output path:

```text
Canon PDL bytes
    -> pdWrite()
    -> cnpkSendData()
    -> write(device_fd, bytes, length)
```

There is no additional Canon CMLP/MLC packet wrapper inserted by `pdWrite()`/`cnpkSendData()` in this path.

## Buffered/process path

When the output context is in the alternate mode, `cnpkSendData()` uses an internal process transport. It maintains a 0x1000-byte buffer and calls `cnprocWriteCommand()` / `cnprocWriteData()` and `cnprocCheckResponse()`.

These records are **internal driver-process IPC framing**, not printer PDL framing. They should not be copied into the Android USB stream unless independent evidence shows that Android is deliberately emulating the driver's internal helper process.

The direct path is the relevant one for understanding a Linux backend that ultimately writes the generated UFR II LT stream to a USB device file.

## Consequence for Android USB work

The recovered Canon code supports the following model:

```text
Pdl_StartJob/Page/Raster/End...
        |
        v
     pdWrite
        |
        v
  cnpkSendData
        |
        +---- direct mode ----> write(device_fd, PDL bytes)
        |
        +---- process mode ---> internal cnproc IPC
```

This substantially reduces uncertainty around the missing USB framing: the UFR II LT PDL itself is written as the payload in the direct backend path. The Android implementation should therefore not add guessed CMLP framing merely because the Linux driver has an internal `cnproc*` path.

The remaining Android-specific question is the exact USB device-file/kernel semantics between Linux `write(device_fd, ...)` and Android's USB bulk endpoint. That should be resolved from the Canon USB backend (`cnusb.c`) and Android USB printer-class behavior before any physical test.

No physical printer has been used for these experiments.
