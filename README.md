# USB Print

A small Android app for direct USB/OTG printing, with no ads and no premium print wall.

## Current milestone

**Phase 2: USB communication + PDF raster pipeline**

The app can:
- Detect USB devices connected through OTG
- Identify printer-class USB devices
- Display USB vendor/product IDs
- Request Android USB permission
- Open and claim the printer interface
- Find bulk USB endpoints
- Verify USB Printer Class communication with `GET_PORT_STATUS`
- Select a PDF using Android's document picker
- Render PDF pages into packed 1-bit monochrome raster data
- Recognize the observed Canon LBP6030/6040/6018L USB family profile (`04A9:2795`)

## Target printer

Initial testing target: Canon LBP6030/6040/6018L family.

The physical USB device was observed identifying itself as `Canon LBP6030/6040/6018L`. The device-specific serial/identifier is intentionally not stored in the static printer profile.

Canon's current Linux UFRII LT driver documentation supports the LBP6030/LBP6030B/LBP6030w family and Linux ARM. The model-specific profile records the observed USB ID and the A4/600-dpi defaults needed by the print pipeline.

## Print architecture

`PDF -> monochrome raster -> SFP job -> verified UFR II LT encoder -> USB bulk transfer -> printer`

The UFR II LT/SFP encoder is still the remaining component before a physical test page can be sent. The project does not bundle Canon's proprietary driver binaries.

Research confirms that the LBP6030 family PPD routes CUPS raster and CUPS command input through `rastertosfp`. This is evidence for the driver pipeline, not by itself a complete protocol specification.

Open-source research shows that Canon's Linux driver family has separate SFP/LT components and mixed licensing/dependencies. We therefore keep the Android implementation modular and only integrate code whose license permits redistribution.

## Safety gate

No guessed UFR II LT/SFP byte stream is sent to the physical printer. The encoder remains fail-closed until deterministic encoded vectors are available and verified.

## Next milestone

Identify the exact `rastertosfp` encoding source path and its redistributable dependencies, implement the verified encoding behavior, validate deterministic vectors, then send a controlled one-page fixed test job over the already-working USB connection.
