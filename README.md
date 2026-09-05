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

## Target printer

Initial testing target: Canon USB printer from the imageCLASS LBP6030 family.

## Print architecture

The target printer uses Canon UFR II LT. PDF bytes or generic raster bytes must not be sent directly to the printer. The application therefore separates PDF rendering from the printer protocol:

`PDF -> monochrome raster -> UFR II LT encoder -> USB bulk transfer -> printer`

The UFR II LT encoder is the remaining component before a physical test page can be sent. The project does not bundle Canon's proprietary driver binaries.

## Next milestone

Implement and validate the UFR II LT encoder for the LBP6030 family, then send a controlled one-page test job over the already-working USB connection.
