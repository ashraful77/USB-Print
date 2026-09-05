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
- Recognize the observed Canon LBP6030B USB profile (`04A9:2795`)

## Target printer

Initial testing target: Canon imageCLASS LBP6030B / LBP6030 family.

Canon's Linux UFRII LT driver documentation identifies this family as UFRII LT. The model-specific profile in this project records the observed USB ID and the A4/600-dpi defaults needed by the print pipeline.

## Print architecture

`PDF -> monochrome raster -> UFR II LT encoder -> USB bulk transfer -> printer`

The UFR II LT encoder is still the remaining component before a physical test page can be sent. The project does not bundle Canon's proprietary driver binaries.

Open-source research shows that Canon's Linux driver family has separate SFP/LT components and that some UFR II LT components are GPL-licensed, while other driver components remain proprietary. We therefore keep the Android implementation modular and only integrate code whose license permits redistribution.

## Next milestone

Implement the verified UFR II LT/SFP encoding path for the LBP6030B, validate its page framing and raster compression, then send a controlled one-page test job over the already-working USB connection.
