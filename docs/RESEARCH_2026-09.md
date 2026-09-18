# Research note — LBP6030B SFP/CPCA

## Conclusion

The earlier JBIG lead has been investigated and downgraded for the LBP6030B target.

The LBP6030 family is officially UFR II LT. Public Linux/CUPS evidence shows the final filter as `rastertosfp` and reports the device identification `CA_UFRIILT_OIP;CMD:LIPSLX,CPCA`. This points toward the SFP/CPCA path rather than the UFR-II JBIG path. Canon's current India support page still lists the LBP6030/LBP6030B/LBP6030w family under UFRII LT drivers and includes Linux ARM support. citeturn0search5

## New external observation

A 2025 LBP6030w Linux/ARM investigation shows CUPS launching `rastertosfp` after conversion to CUPS raster. The investigator later reported that even after compiling the filter for ARM, the printer still did not print and eventually observed that the printer disconnected/reset when a print command was issued. This is useful negative evidence: obtaining the filter is not enough to establish the complete USB stream path. citeturn0search0turn0search4

## CPCA boundary finding

A public Canon package layout exposes an older `pstoncapcpca` component in the binary driver package, while current UFRII LT packaging installs `rastertosfp` as the active filter. This suggests the CPCA-related path has changed across driver generations, so we should not assume that an older CPCA component emits the same stream as the LBP6030B's current SFP filter.

Searches for an independent LBP6030 SFP encoder continue to return packaging metadata rather than a complete independently licensed encoder. The open-source CAPT driver is explicitly for CAPT-based printers and documents a different reverse-engineered protocol family; it is therefore not being substituted for UFRII LT/SFP. citeturn0search7

## Engineering decision

Keep the encoder fail-closed.

The next implementation target is a clean-room SFP/CPCA encoder derived only from independently redistributable source/specification and deterministic test vectors. Canon proprietary `rastertosfp` binaries and proprietary runtime components will not be copied into the Android application.

## Next experiment

1. Trace historical `pstoncapcpca` versus current `rastertosfp` behavior.
2. Search for independently licensed CPCA framing code, not merely CAPT code.
3. Separate generic raster/compression functionality from Canon-specific framing.
4. Build deterministic vectors only from independently sourced behavior.
5. Keep USB output disabled until a verified encoder exists.

No guessed printer bytes will be enabled.
