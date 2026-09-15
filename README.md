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

Canon's Linux UFRII LT driver documentation identifies this family as UFRII LT. The model-specific profile in this project records the observed USB ID and the A4/600-dpi defaults used by the print pipeline.

## Print architecture

`PDF -> monochrome raster -> encoder interface -> USB bulk transfer -> printer`

The UFR II LT encoder is currently **not implemented**. The project intentionally fails closed: it will not generate or send unverified printer protocol bytes.

No Canon proprietary binaries are bundled or reconstructed by the Android build.

## Research boundary

Earlier reverse-engineering experiments identified implementation details in Canon's Linux driver package. Those findings are retained as research notes only and are not used to generate printer output in the application.

A future encoder may be integrated only when its implementation has a clear, redistributable source or an independently developed clean-room basis suitable for inclusion in this project.

## Next milestone

Find a legitimately redistributable UFR II LT encoder implementation or a sufficiently documented public protocol specification. Then add deterministic offline tests before considering any physical printer test.
