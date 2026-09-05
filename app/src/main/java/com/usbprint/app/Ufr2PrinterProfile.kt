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
    val driverFamily: DriverFamily,
    val usbProductName: String = name
) {
    enum class DriverFamily {
        SFP
    }

    companion object {
        /**
         * Canon LBP6030 family profile.
         *
         * The physical device can identify itself as:
         * "Canon LBP6030/6040/6018L".
         * The serial number is device-specific and is intentionally not part
         * of the profile.
         */
        val LBP6030B = Ufr2PrinterProfile(
            name = "Canon LBP6030/6040/6018L",
            vendorId = 0x04A9,
            productId = 0x2795,
            defaultDpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297,
            driverFamily = DriverFamily.SFP,
            usbProductName = "Canon LBP6030/6040/6018L"
        )

        fun fromUsbIds(vendorId: Int, productId: Int): Ufr2PrinterProfile? =
            if (vendorId == LBP6030B.vendorId && productId == LBP6030B.productId) {
                LBP6030B
            } else {
                null
            }
    }
}
