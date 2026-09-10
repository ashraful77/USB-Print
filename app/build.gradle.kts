import java.util.Base64
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
}

// The Canon ARM64 SLIM compressor is kept as compressed text so the repository
// remains source-friendly. It is reconstructed into jniLibs before native merge.
val unpackCanonSlim = tasks.register("unpackCanonSlim") {
    val encoded = layout.projectDirectory.file("src/main/canon_slimsfp.so.gz.b64").asFile
    val output = layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libcanon_slimsfp.so").asFile
    inputs.file(encoded)
    outputs.file(output)
    doLast {
        output.parentFile.mkdirs()
        val compressed = Base64.getDecoder().decode(encoded.readText().trim())
        GZIPInputStream(compressed.inputStream()).use { input ->
            output.outputStream().use { out -> input.copyTo(out) }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(unpackCanonSlim)
}

android {
    namespace = "com.usbprint.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.usbprint.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
