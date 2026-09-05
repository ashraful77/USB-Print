package com.usbprint.app

/**
 * Model-specific information needed by the UFR II LT print pipeline.
 *
 * The USB ID is useful for selecting the correct Canon driver profile, but it
 * does not by itself define a complete UFR II LT byte stream. The encoder must
 * still be validated against real output from a compatible driver.
 */
data class Ufr2PrinterProfile(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val defaultDpi: Int,
    val paperWidthMm: Int,
    val paperHeightMm: Int,
    val driverFamily: DriverFamily
) {
    enum class DriverFamily {
        SFP
    }

    companion object {
        /** Canon LBP6030B USB ID observed by the app. */
        val LBP6030B = Ufr2PrinterProfile(
            name = "Canon imageCLASS LBP6030B",
            vendorId = 0x04A9,
            productId = 0x2795,
            defaultDpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297,
            driverFamily = DriverFamily.SFP
        )

        fun fromUsbIds(vendorId: Int, productId: Int): Ufr2PrinterProfile? =
            if (vendorId == LBP6030B.vendorId && productId == LBP6030B.productId) {
                LBP6030B
            } else {
                null
            }
    }
}
