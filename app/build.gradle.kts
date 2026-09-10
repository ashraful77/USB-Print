import java.util.Base64
import java.util.zip.GZIPInputStream

plugins {
    id("com.android.application")
}

// The Canon ARM64 SLIM compressor is stored as four text chunks so it can be
// kept in the repository through the text-only GitHub contents API. Gradle
// reconstructs the original gzip payload before the native merge step.
val unpackCanonSlim = tasks.register("unpackCanonSlim") {
    val sourceDir = layout.projectDirectory.dir("src/main").asFile
    val chunks = fileTree(sourceDir) {
        include("canon_slimsfp.*.b64")
    }.files.sortedBy { it.name }
    val output = layout.projectDirectory.file("src/main/jniLibs/arm64-v8a/libcanon_slimsfp.so").asFile
    inputs.files(chunks)
    outputs.file(output)
    doLast {
        require(chunks.size == 4) { "Canon SLIM payload is incomplete: expected 4 chunks, found ${chunks.size}" }
        output.parentFile.mkdirs()
        val encoded = buildString {
            chunks.forEach { append(it.readText().trim()) }
        }
        val compressed = Base64.getDecoder().decode(encoded)
        GZIPInputStream(compressed.inputStream()).use { input ->
            output.outputStream().use { out -> input.copyTo(out) }
        }
        require(output.length() > 30000) { "Reconstructed Canon SLIM library is unexpectedly small" }
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
