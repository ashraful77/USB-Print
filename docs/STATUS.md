# Status

- Android PDF selection and rasterization remain.
- USB printer communication remains isolated from printer-language encoding.
- UFR II LT/SFP encoding is intentionally disabled.
- No Canon proprietary binary is reconstructed, bundled, or executed by this branch.
- No guessed printer bytes are sent.

## Current finding

The LBP6030 family should not be treated as one of the UFR-II JBIG targets from the public carps-cups research. In that project's May 2026 discussion, the researcher describes the LBP6030 as one of the UFRII-LT/SFP models whose compression/protocol path had not been established there, and notes it may use a newer CAPT-like implementation.

A separate public Linux/CUPS log for the LBP6030 reports the device identification string as CID:CA_UFRIILT_OIP;CMD:LIPSLX,CPCA and shows Canon's rastertosfp as the final filter. This is useful architectural evidence, but it is not a complete encoder specification.

## Blocker

A complete, independently sourced, redistributable LBP6030B UFR II LT/SFP encoder has not been verified.

The most promising next route is therefore protocol/specification research around CPCA/SFP, not the UFR-II JBIG path.

## Rules

- Do not send guessed headers, raw PDF, or raw raster data to the printer.
- Do not bundle Canon proprietary binaries.
- Do not treat proprietary Canon runtime behavior as a redistributable implementation.
- Keep the hardware print path fail-closed until framing and encoding are independently verified.
