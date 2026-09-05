# Encoder blocker and legal boundary

## Why the APK still does not print

The Android USB transport and raster pipeline are ready, but the project does not yet contain a verified Canon LBP6030B SFP/UFRII LT encoder. `Ufr2Encoder` therefore remains fail-closed and returns no printer bytes until a verified encoder implementation exists.

Canon's LBP6030 family support page currently lists the Linux UFRII LT driver V5.10 for LBP6030/LBP6030B/LBP6030w, and Canon's driver page lists Linux ARM support and CUPS as the printing system.

## License boundary

Canon's published Linux V5.00 driver license grants use of the Canon software under its stated terms but restricts transfer and, in the published disclaimer, prohibits reproducing, modifying, disassembling, decompiling, reverse engineering, or creating derivative works from the supplied content. The same page separately identifies GPL-licensed free-software components.

For USB-Print we therefore do **not** copy Canon binaries, extract proprietary implementation code, or build an encoder by reverse engineering Canon's proprietary driver package.

## Safe implementation path

1. Prefer a clearly licensed, self-contained SFP implementation whose license permits inclusion in this project.
2. If no such implementation exists, keep the Android encoder fail-closed rather than guessing printer-language bytes.
3. Any future encoder must pass deterministic raster-to-output tests and then physical validation on the LBP6030B.
4. Keep the USB layer restricted to encoder output; PDF or raw raster data must never be sent directly to the printer.

## Current status

**Printing remains intentionally disabled.** This is a correctness and licensing boundary, not a USB connectivity problem.

## References

- Canon LBP6030/LBP6030B/LBP6030w support page
- Canon UFRII LT Linux driver documentation and license
