# USB Print

A small Android app for direct USB/OTG printing, with no ads and no premium print wall.

## Current milestone

**Phase 2: USB communication + PDF raster pipeline**

The app currently can:
- Detect USB devices connected through OTG
- Identify printer-class USB devices
- Display USB vendor/product IDs
- Request Android USB permission
- Open and claim the printer interface
- Find bulk USB endpoints
- Verify USB Printer Class communication with `GET_PORT_STATUS`
- Select a PDF using Android's document picker
- Render PDF pages into packed 1-bit monochrome raster data
- Recognize the observed Canon LBP6030B USB profile (`04A9:2795`)

## Target printer

Initial testing target: **Canon imageCLASS LBP6030B / LBP6030 family**.

The target uses Canon's UFR II LT/SFP print pipeline. The observed LBP6030B USB profile is `04A9:2795`, with the current raster pipeline targeting A4 at 600 dpi.

## Print architecture

`PDF -> monochrome raster -> UFR II LT/SFP encoder -> USB bulk transfer -> printer`

The USB and PDF/raster stages are implemented. The **UFR II LT/SFP encoder remains the main blocker** before any physical print job can be sent.

## Encoder status

The project intentionally **fails closed** while a verified, redistributable encoder is unavailable.

No guessed or experimental UFR II LT/SFP byte stream is sent to the physical printer.

Canon driver research has established the Linux-side architecture as broadly:

`CUPS raster -> rastertosfp -> Canon SFP/UFR II LT runtime -> USB`

The research also identified proprietary Canon runtime components used by the Linux driver. Those components are **not bundled with this Android project**, and proprietary payloads previously present in development history are being removed from the distributable implementation.

Reverse-engineering observations are kept separate from the shipped encoder. They are not treated as a substitute for an independently licensed implementation or a clearly redistributable specification.

## Safety and development policy

- Never fake a successful print.
- Never send guessed/experimental bytes to the physical printer.
- Keep the hardware print path disabled until the encoder and framing are verified.
- Validate deterministic host-side test vectors before hardware testing.
- Use a controlled one-page monochrome test pattern before PDF printing.
- Do not bundle Canon proprietary binaries or source without clear redistribution rights.
- Keep the Android implementation modular so a legitimate encoder can be integrated later.

## Current state

**Working:** USB OTG detection, permission, interface claiming, endpoints, USB Printer Class status communication, PDF selection, PDF rasterization, validation, and safety gates.

**Not yet implemented:** a verified, independently usable UFR II LT/SFP raster encoder for the LBP6030B.

Therefore, the app **does not currently claim to print successfully to the LBP6030B**. This is intentional until the missing encoder is solved.

## Next milestone

Find or develop a clearly redistributable UFR II LT/SFP encoding implementation, establish deterministic golden vectors and page framing, validate the complete generated job, and only then enable a controlled one-page physical test over the already-working USB connection.

## Research status

The project remains an active research effort. If no legitimate/self-contained encoder path can be established, the correct state is to remain fail-closed rather than ship an unverified printer protocol implementation.
