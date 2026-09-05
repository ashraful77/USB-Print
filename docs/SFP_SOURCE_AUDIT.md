# SFP / UFR II LT source audit

## Current evidence

Canon's current Linux UFRII LT driver explicitly supports the LBP6030/LBP6030B/LBP6030w family. Canon describes the Linux driver as CUPS-based and lists Linux ARM support.

A public GPL-2.0 Canon UFR II driver source tree (`vicamo/cndrvcups-lb`) contains the `pstoufr2cpca` CUPS filter and UFR II PPDs. Its README states that the filter converts PostScript into printer-readable UFR II form.

## Important limitation

This public source tree is **not yet proven to be the exact SFP implementation required by the LBP6030B**. The LBP6030B has already been identified in this project as an SFP-family target, so we must not substitute a generic UFR II implementation and assume compatibility.

The PPD/filter architecture is useful evidence for the next investigation: the printer-specific encoding may be isolated from the CUPS front end, but this must be demonstrated from source and licensing information before porting anything into Android.

## Porting rule

Do not copy Canon proprietary libraries into the APK. Do not port code whose license does not permit redistribution. Do not enable physical printing based only on matching model names or PPD metadata.

## Next gate

1. Inspect the public filter and its dependencies.
2. Identify which code actually constructs printer-language output.
3. Determine whether that code is GPL-compatible and self-contained enough for ARM64 Android.
4. If it is not sufficient, use an authorized/known-good driver output as a golden-vector reference and implement only verified behavior.
5. Keep `Ufr2Encoder` fail-closed until the implementation passes deterministic vectors and physical validation.

## References

- Canon LBP6030 family support page: https://in.canon/en/support/imageCLASS%20LBP6030__%20LBP6030B__%20LBP6030w/model
- Canon UFRII LT Linux V5.10 driver: https://in.canon/en/support/0100595001?model=imageCLASS+LBP6230dn
- Public GPL UFR II source: https://github.com/vicamo/cndrvcups-lb
