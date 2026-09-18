# Research note — LBP6030B SFP/CPCA

## Conclusion

The earlier JBIG lead has been investigated and downgraded for the LBP6030B target.

The LBP6030 family is officially UFR II LT. Public Linux/CUPS evidence shows the final filter as rastertosfp and reports the device identification CA_UFRIILT_OIP;CMD:LIPSLX,CPCA. This points toward the SFP/CPCA path rather than the UFR-II JBIG path.

A public UFR-II JBIG discussion also distinguishes the LBP6030 from the JBIG model list and suggests the LBP6030/LBP6230/LBP7110C/LBP8100 group may use a newer CAPT-like implementation.

This is enough to redirect research, but not enough to generate safe printer bytes.

## Engineering decision

Keep the encoder fail-closed.

The next implementation target is a clean-room SFP/CPCA encoder derived only from independently redistributable source/specification and deterministic test vectors. Canon proprietary rastertosfp binaries and other proprietary runtime components will not be copied into the Android application.
