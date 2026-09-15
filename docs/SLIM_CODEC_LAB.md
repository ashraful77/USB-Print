# SLIM codec differential-analysis lab

Research-only. This lab does not send data to a printer and does not contain Canon proprietary binaries.

## Purpose

Reduce the remaining UFR II LT SFP problem to reproducible compression vectors. The Canon reference observations currently available are:

- Raster: 4958 pixels × 256 lines, 1 bit/pixel.
- Input bytes/line: 620.
- Compression parameters: `03 00 09 00 06 01 50 00`.
- `lCaptCompEx()` blank-band result: 808 bytes, 256 encoded lines.
- Blank final SLIM record SHA-256: `173fb7161d05e7184a163ccf4d91b5186311f3c14b6780becb8b404583cc5723`.
- Vertical-stripe final record SHA-256: `da51e34da9f45eeead6a2d4bc42d17d694f56b4e1cc47dad472340cf479ede42`.
- 16×16 checker final record SHA-256: `aecdb13022fce4af7d5eb2567104bd3a3355d16996ba0e6458c8e8736e306f60`.

## Differential vector matrix

The next host-side reference capture should use the same dimensions and parameters for each vector:

1. all white
2. first input bit set
3. last input bit set
4. first byte `FF`
5. one complete black row
6. two identical rows
7. alternating black/white rows
8. repeated 16-byte pattern
9. one changed byte in the middle
10. two distant changed bytes

For every vector record:

```text
compressed length
encoded line count
COMPPARAM (8 bytes)
first 64 compressed bytes
first 64 final SLIM bytes
SHA-256 of final SLIM bytes
```

## Safety boundary

Do not add Canon SLIM binaries, proprietary driver payloads, or guessed printer packets. Do not enable physical transmission from this lab. A clean-room implementation must first pass independently generated golden vectors.

## Transport finding

The recovered direct Canon path is `pdWrite -> cnpkSendData -> write(device_fd, ...)`; the internal `cnproc*` records are not assumed to be printer framing. See `PDWRITE_TRANSPORT_ANALYSIS.md`.
