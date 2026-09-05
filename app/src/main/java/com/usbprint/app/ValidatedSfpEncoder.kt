package com.usbprint.app

/**
 * Validation gate for any real SFP/UFR II LT encoder.
 *
 * The encoder is never called until the job has passed the LBP6030B profile
 * checks. This keeps malformed raster input out of the printer-format layer.
 */
class ValidatedSfpEncoder(
    private val delegate: SfpEncoder,
    private val profile: Ufr2PrinterProfile = Ufr2PrinterProfile.LBP6030B
) : SfpEncoder {
    override val isAvailable: Boolean
        get() = delegate.isAvailable

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result {
        val validation = SfpJobValidator.validate(job, profile)
        if (!validation.valid) {
            return Ufr2Encoder.Result(
                success = false,
                data = null,
                message = validation.message
            )
        }
        return delegate.encode(job)
    }
}
