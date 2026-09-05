# SFP / UFR II LT source audit

## Current evidence

Canon's current Linux UFRII LT driver explicitly supports the LBP6030/LBP6030B/LBP6030w family. Canon describes the Linux driver as CUPS-based and lists Linux ARM support.

A public GPL-2.0 Canon UFR II driver source tree (`vicamo/cndrvcups-lb`) contains the `pstoufr2cpca` CUPS filter and UFR II PPDs. Its README states that the filter converts PostScript into printer-readable UFR II form.

## What the public source tells us

The public `pstoufr2cpca` component is a CUPS filter/front end. Its source accepts CUPS/PPD job information and launches Ghostscript with UFR II-specific device/model arguments; it is not, by itself, a self-contained Android printer-language encoder.

The accompanying UFR II PPDs also describe printer-specific components such as `ufr2filter`, `libcanonufr2`, and `libcanonc3pl`. This is strong evidence that the public filter tree depends on additional Canon components for the final printer-language path rather than containing a complete portable encoder in the filter source alone.

The public repository therefore gives us useful architecture and metadata, but it does **not** currently give us a verified, self-contained SFP encoder that can safely be dropped into the APK.

## Important limitation

This public source tree is **not yet proven to be the exact SFP implementation required by the LBP6030B**. The LBP6030B has already been identified in this project as an SFP-family target, so we must not substitute a generic UFR II implementation and assume compatibility.

In particular, the presence of a UFR II PPD or the `pstoufr2cpca` filter is not enough to prove that its output is accepted by the LBP6030B's SFP firmware.

## Porting rule

Do not copy Canon proprietary libraries into the APK. Do not port code whose license does not permit redistribution. Do not enable physical printing based only on matching model names, PPD metadata, or a generic UFR II filter.

## Next gate

1. Obtain an authorized/known-good LBP6030B SFP driver output for a deterministic test page, or identify a clearly licensed self-contained SFP implementation.
2. Compare the resulting byte stream against our 1-bit 600-dpi raster input and isolate the actual page/job encoding layer.
3. Build deterministic golden-vector tests from verified output.
4. Port only the verified, redistributable encoder path to ARM64 Android.
5. Keep `Ufr2Encoder` fail-closed until deterministic vectors and physical validation both pass.

## Current decision

**Do not implement guessed UFR II/SFP packet bytes yet.** The evidence now points to the correct next milestone: acquire a verified SFP output reference or a clearly redistributable self-contained encoder, rather than spending more time on speculative protocol construction.

## References

- Canon LBP6030 family support page: https://in.canon/en/support/imageCLASS%20LBP6030__%20LBP6030B__%20LBP6030w/model
- Canon UFRII LT Linux V5.10 driver: https://in.canon/en/support/0100595001?model=imageCLASS+LBP6230dn
- Public GPL UFR II source: https://github.com/vicamo/cndrvcups-lb
