# Research note — LBP6030B SFP/CPCA

## Conclusion

The earlier JBIG lead has been investigated and downgraded for the LBP6030B target.

The LBP6030 family is officially UFR II LT. Public Linux/CUPS evidence shows the final filter as `rastertosfp` and reports the device identification `CA_UFRIILT_OIP;CMD:LIPSLX,CPCA`. This points toward the SFP/CPCA path rather than the UFR-II JBIG path.

A public UFR-II JBIG discussion also distinguishes the LBP6030 from the JBIG model list and suggests the LBP6030/LBP6230/LBP7110C/LBP8100 group may use a newer CAPT-like implementation.

## New external observation

A 2025 LBP6030w Linux/ARM investigation provides an important negative result: the Canon `rastertosfp` filter could be compiled for ARM, but the printer still did not print and no obvious CUPS error remained. This shows that merely obtaining or rebuilding the filter is not enough to establish the complete USB stream path.

The same logs show CUPS launching `rastertosfp` before the USB backend and identify the printer as `CA_UFRIILT_OIP;CMD:LIPSLX,CPCA`. This reinforces that `rastertosfp` is the critical conversion boundary, while leaving its complete runtime dependencies and emitted stream semantics unresolved.

## Engineering decision

Keep the encoder fail-closed.

The next implementation target is a clean-room SFP/CPCA encoder derived only from independently redistributable source/specification and deterministic test vectors. Canon proprietary `rastertosfp` binaries and other proprietary runtime components will not be copied into the Android application.

## Next experiment

Focus on dependency tracing and externally observable behavior:

1. map `rastertosfp` dependencies from public packaging/build metadata;
2. identify which dependency performs actual stream emission;
3. separate generic raster/JBIG functionality from Canon-specific SFP framing;
4. search for an independently licensed implementation of the required framing;
5. only then add a deterministic encoder test vector.

No guessed printer bytes will be enabled.
