# UFR II LT / SFP implementation plan

## Verified facts

- Canon identifies the LBP6030/LBP6030B/LBP6030w as UFR II LT.
- Canon's Linux UFRII LT driver supports the LBP6030 family.
- The Linux driver architecture for this family uses a rastertosfp filter.
- Public Linux printer logs identify the LBP6030 device as CA_UFRIILT_OIP with CMD:LIPSLX,CPCA.
- PDF bytes and generic packed raster bytes are not valid printer jobs.

## Important correction

The public ondrej-zary/carps-cups UFR-II JBIG investigation is not currently a solution for LBP6030B. Its May 2026 discussion lists many JBIG models, but the LBP6030 is discussed separately as an UFRII-LT/SFP model that may use a newer CAPT-like path.

Therefore we should not implement JBIG framing and assume it will print on LBP6030B.

## Current milestone

1. PDF is rendered to 1-bit monochrome raster data.
2. USB printer communication is isolated.
3. Ufr2Encoder is an explicit fail-closed boundary.
4. Research has narrowed the missing layer to the LBP6030 SFP/CPCA path.
5. Next task: identify an independently usable CPCA/SFP specification or clean-room implementation and build deterministic golden vectors.

## Candidate research material

- Libre CAPT projects are useful for understanding Canon's older CAPT architecture, but their supported models do not establish compatibility with LBP6030B.
- Public Canon driver packaging confirms that rastertosfp is a proprietary runtime component for the LBP6030 family; it is not being bundled here.
- The LBP6030 USB identity 04A9:2795 and its public device identification can be used as the hardware profile.

## Rules

Do not send guessed bytes to the physical printer.
Do not copy proprietary Canon runtime code into this repository.
Do not enable printing merely because a stream looks structurally plausible.
