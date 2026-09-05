# SFP encoder requirements

## Target

Canon imageCLASS LBP6030B (USB ID `04A9:2795`). Canon identifies the LBP6030/LBP6030B/LBP6030w family as using its UFRII LT printer driver. The current Canon Linux driver is V5.10.

## Safe implementation boundary

The application pipeline is:

`PDF -> monochrome raster -> SFP/UFRII LT encoder -> encoded job -> USB bulk OUT`

The USB layer must accept only encoder output. It must never receive a PDF or generic raster as a print job.

## Encoder requirements

1. Accept validated 1-bit raster pages.
2. Encode the model's SFP/UFRII LT job format.
3. Return an explicit failure when the encoder cannot produce a verified job.
4. Remain independent from Android USB transport.
5. Be portable to ARM64 Android without depending on a full desktop CUPS installation.
6. Include deterministic unit/golden-vector tests before enabling physical printing.

## What is deliberately excluded

Do not copy Canon proprietary driver binaries into the APK. Do not invent protocol headers or send guessed bytes to a physical printer.

## Validation gate

The Print UI remains disabled until at least one encoder implementation has a verified golden vector and the generated stream has been validated against an authorized/known-good driver output for the LBP6030B.

## Current state

The JNI/native boundary and USB transfer path are implemented, but the SFP encoder itself is not implemented. This document defines the next implementation target without pretending that the printer protocol is already solved.
