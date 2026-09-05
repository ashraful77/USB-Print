# SFP / UFR II LT source audit

## Current evidence

Canon's current Linux UFRII LT driver explicitly supports the LBP6030/LBP6030B/LBP6030w family. Canon describes the Linux driver as CUPS-based and lists Linux ARM support. The current Canon driver page was updated 20-Aug-2026.

A public GPL-2.0 Canon UFR II driver tree (`vicamo/cndrvcups-lb`) contains the `pstoufr2cpca` CUPS filter and UFR II PPDs. Its README states that the filter converts PostScript into printer-readable UFR II form.

## LBP6030-specific SFP evidence

A publicly indexed LBP6030 PPD identifies the model as `Canon LBP6030/6040/6018L` and routes both CUPS raster and CUPS command input through a filter named `rastertosfp`. This is useful evidence that the LBP6030 family uses an SFP-specific raster filter in the Linux printing pipeline.

A separate public Gentoo packaging recipe for Canon's UFRII-LT binary package also explicitly installs `rastertosfp` as a CUPS filter. This corroborates that `rastertosfp` is an actual driver component rather than a guessed protocol name.

However, these public references do **not** provide a verified, self-contained, redistributable implementation of `rastertosfp`. They only establish the filter's role/name in the known-good Linux driver stack.

## What the public source tells us

The public `pstoufr2cpca` component is a CUPS filter/front end. Its source accepts CUPS/PPD job information and launches Ghostscript with UFR II-specific device/model arguments; it is not, by itself, a self-contained Android printer-language encoder.

The accompanying UFR II PPDs also describe printer-specific components such as `ufr2filter`, `libcanonufr2`, and `libcanonc3pl`. This is strong evidence that the public filter tree depends on additional Canon components for the final printer-language path rather than containing a complete portable encoder in the filter source alone.

The public repository therefore gives us useful architecture and metadata, but it does **not** currently give us a verified, self-contained SFP encoder that can safely be dropped into the APK.

## Important limitation

This public source tree is **not yet proven to be the exact SFP implementation required by the LBP6030B**. The LBP6030B has already been identified in this project as an SFP-family target, so we must not substitute a generic UFR II implementation and assume compatibility.

In particular, the presence of a UFR II PPD, the `pstoufr2cpca` filter, or the name `rastertosfp` is not enough to prove that an implementation of the filter is legally redistributable or that its output is accepted by the LBP6030B's SFP firmware.

## Porting rule

Do not copy Canon proprietary libraries into the APK. Do not port code whose license does not permit redistribution. Do not enable physical printing based only on matching model names, PPD metadata, or a generic UFR II filter.

## Next gate

1. Obtain an authorized/known-good LBP6030B SFP driver output for a deterministic test page, or identify a clearly licensed self-contained SFP implementation.
2. Determine whether a redistributable implementation of the `rastertosfp` stage exists in public source; if not, do not recreate proprietary protocol details by guesswork.
3. Compare any legally usable reference implementation/output against our 1-bit 600-dpi raster input and isolate the actual page/job encoding layer.
4. Build deterministic golden-vector tests from verified output.
5. Port only the verified, redistributable encoder path to ARM64 Android.
6. Keep `Ufr2Encoder` fail-closed until deterministic vectors and physical validation both pass.

## Current decision

**Do not implement guessed UFR II/SFP packet bytes yet.** The evidence now points to the correct next milestone: acquire a verified SFP output reference or a clearly redistributable self-contained encoder, rather than spending more time on speculative protocol construction.

## References

- Canon LBP6030 family support page: https://in.canon/en/support/imageCLASS%20LBP6030__%20LBP6030B__%20LBP6030w/model
- Canon UFRII LT Linux V5.10 driver: https://in.canon/en/support/0100595001?model=imageCLASS+LBP6230dn
- Canon LBP6030B specifications: https://in.canon/en/consumer/imageclass-lbp6030b/main/specification
- Public GPL UFR II source: https://github.com/vicamo/cndrvcups-lb
- Public LBP6030 PPD evidence for `rastertosfp`: https://github.com/DangXuanThong/nixos-dotfiles/blob/b5c4af20a30457fc2cf5107c2e5c159e716e14c7/printers/Canon/LBP6030/ppd/CNRCUPSLBP6030ZNK.ppd
