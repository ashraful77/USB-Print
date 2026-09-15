# LBP6030 USB Transport Notes — 2026-09-15

Research-only note based on the Canon UFRII LT V5.10 source package. No proprietary Canon binary is included.

## Canon Linux backend behavior

The open `cnusb.c` backend ultimately opens the printer device and writes the generated print stream directly to the device file. Its main transfer loop calls the normal POSIX `write(device_fd, print_ptr, print_bytes)` path.

The source does not add an application-level packet wrapper around the UFR II LT stream at this layer. The device-file/kernel USB printer path is responsible for carrying the bytes to the printer.

The backend also uses exclusive open semantics (`O_RDWR | O_EXCL`) and reports write errors such as offline or media-empty conditions.

## Implication for Android

Our Android implementation already operates below the CUPS/usblp layer, using Android USB host bulk endpoints. Therefore we should conceptually separate two layers:

```text
UFR II LT / SFP byte stream
        |
        v
USB printer transport
```

The first layer must be reconstructed byte-for-byte. The second layer must reproduce the actual USB bulk transfer behavior required by the printer interface; it should not invent an additional UFR packet format merely because the Android app is bypassing usblp.

The Linux backend evidence is useful because it strongly suggests that the Canon-generated stream itself is the payload sent to the USB printer device.

## Current blocker

This does **not** prove that any arbitrary UFR/SFP stream can be written directly to the LBP6030B from Android. We still need to verify:

1. exact job/page initialization records;
2. exact raster-band records and compression;
3. exact end-page/end-job records;
4. printer-class endpoint direction and transfer behavior;
5. status/error sequencing around writes.

No physical print test should be attempted until the complete host-side stream is deterministic.
