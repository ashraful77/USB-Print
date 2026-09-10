#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <initializer_list>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "USBPrintUFR2", __VA_ARGS__)

namespace {
using Byte = uint8_t;
using SlimCompFn = int (*)(Byte*, Byte*, int, int, int, int, int*, void*, int, void*);
struct CompParam { Byte xOffset[2]{}; Byte yOffset[2]{}; int8_t zOffset[2]{}; uint16_t farOffset{}; };
struct NativeResult { bool ok; std::vector<Byte> data; std::string message; };

jobject makeResult(JNIEnv* env, const NativeResult& result) {
    jclass cls = env->FindClass("com/usbprint/app/Ufr2Encoder$Result");
    if (!cls) return nullptr;
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(Z[BLjava/lang/String;)V");
    if (!ctor) return nullptr;
    jbyteArray bytes = nullptr;
    if (result.ok) {
        bytes = env->NewByteArray(static_cast<jsize>(result.data.size()));
        if (!bytes) return nullptr;
        if (!result.data.empty()) env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(result.data.size()), reinterpret_cast<const jbyte*>(result.data.data()));
    }
    jstring msg = env->NewStringUTF(result.message.c_str());
    jobject out = env->NewObject(cls, ctor, static_cast<jboolean>(result.ok), bytes, msg);
    if (bytes) env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(msg);
    return out;
}

void put16be(std::vector<Byte>& out, int value) {
    out.push_back(static_cast<Byte>((value >> 8) & 0xff)); out.push_back(static_cast<Byte>(value & 0xff));
}
void put32le(std::vector<Byte>& out, int value) {
    uint32_t v = static_cast<uint32_t>(value);
    out.push_back(static_cast<Byte>(v & 0xff)); out.push_back(static_cast<Byte>((v >> 8) & 0xff));
    out.push_back(static_cast<Byte>((v >> 16) & 0xff)); out.push_back(static_cast<Byte>((v >> 24) & 0xff));
}
void append(std::vector<Byte>& out, std::initializer_list<int> values) { for (int v : values) out.push_back(static_cast<Byte>(v)); }

std::vector<Byte> beginJob(int dpi) {
    std::vector<Byte> out;
    append(out, {0x01,0xC1,0x85,0x10,0x00,0x10,0x89,0xC2,0x00,0xD8,0x84});
    put16be(out, dpi);
    append(out, {0xDD,0x80,0xC8,0xF0,0x84,0x08,0x00,0x02});
    return out;
}
std::vector<Byte> beginMedia() { return {0x02,0xC3,0x00,0xC5,0x00,0xC6,0x00}; }
std::vector<Byte> paperSource() { return {0x51,0xF2,0x00}; }
std::vector<Byte> prepare() { return {0x61,0xE6,0x80,0x02,0xE5,0x00}; }
std::vector<Byte> beginPage() { return {0x03,0xE7,0x85,0x13,0x80,0x1B,0x68,0xDE,0x80,0x00,0xC8,0x00,0xCA,0xA1,0x00,0x00,0xCB,0x00}; }

std::vector<Byte> transferHeader(int lines, int dataLength) {
    std::vector<Byte> out;
    // Repeat A4 width and band height in both HB geometry fields.
    append(out, {0x62,0xE3,0x85,0x13,0x80}); put16be(out, lines);
    append(out, {0xE8,0xA5,0x13,0x80}); put16be(out, lines);
    append(out, {0xE1,0x00,0xD7,0x84});
    put16be(out, dataLength); append(out, {0x9D,0x03}); put16be(out, dataLength);
    return out;
}

std::vector<Byte> oneBitToTwoBit(const jbyte* raster, int width, int height) {
    const int srcBpr = (width + 7) / 8, dstBpr = (width + 3) / 4;
    std::vector<Byte> out(static_cast<size_t>(dstBpr) * height, 0);
    for (int y = 0; y < height; ++y) {
        const Byte* src = reinterpret_cast<const Byte*>(raster) + static_cast<size_t>(y) * srcBpr;
        Byte* dst = out.data() + static_cast<size_t>(y) * dstBpr;
        for (int x = 0; x < width; ++x) {
            const bool black = ((src[x >> 3] >> (7 - (x & 7))) & 1) != 0;
            const int value = black ? 0 : 3;
            dst[x >> 2] = static_cast<Byte>(dst[x >> 2] | (value << (6 - 2 * (x & 3))));
        }
    }
    return out;
}

void appendCmlpFrame(std::vector<Byte>& out, const Byte* payload, size_t n) {
    const size_t total = n + 6;
    if (total > 0xffff) throw std::runtime_error("CMLP frame too large");
    out.push_back(0x01); out.push_back(0x10);
    out.push_back(static_cast<Byte>((total >> 8) & 0xff)); out.push_back(static_cast<Byte>(total & 0xff));
    out.push_back(0x01); out.push_back(0x00);
    out.insert(out.end(), payload, payload + n);
}

void appendCmlpFrames(std::vector<Byte>& out, const std::vector<Byte>& pdl) {
    constexpr size_t CHUNK = 0x1000;
    for (size_t pos = 0; pos < pdl.size();) {
        const size_t n = std::min(CHUNK, pdl.size() - pos);
        appendCmlpFrame(out, pdl.data() + pos, n);
        pos += n;
    }
    static const Byte flush[] = {0x08,0x00,0x00,0x00};
    appendCmlpFrame(out, flush, sizeof(flush));
}

NativeResult encode(const jbyte* raster, int width, int height, int dpi) {
    if (!raster || width <= 0 || height <= 0) return {false, {}, "Invalid raster"};
    if (dpi != 600) return {false, {}, "Canon LBP6030B SFP path currently requires 600 DPI"};

    void* handle = dlopen("libcanon_slimsfp.so", RTLD_NOW | RTLD_LOCAL);
    if (!handle) {
        const char* err = dlerror();
        return {false, {}, std::string("Cannot load Canon SLIM library: ") + (err ? err : "unknown error")};
    }
    auto comp = reinterpret_cast<SlimCompFn>(dlsym(handle, "lCaptCompEx"));
    if (!comp) {
        const char* err = dlerror(); dlclose(handle);
        return {false, {}, std::string("Canon SLIM lCaptCompEx is unavailable: ") + (err ? err : "unknown error")};
    }

    try {
        const auto twoBit = oneBitToTwoBit(raster, width, height);
        const int srcBpr = (width + 3) / 4, stripes = (height + 255) / 256;
        std::vector<Byte> pdl;
        auto add = [&](const std::vector<Byte>& b) { pdl.insert(pdl.end(), b.begin(), b.end()); };
        add(beginJob(dpi)); add(beginMedia()); add(paperSource()); add(beginPage()); add(prepare());

        for (int band = 0; band < stripes; ++band) {
            const int line0 = band * 256, lines = std::min(256, height - line0);
            std::vector<Byte> input(static_cast<size_t>(srcBpr) * lines);
            std::memcpy(input.data(), twoBit.data() + static_cast<size_t>(line0) * srcBpr, input.size());
            const int capacity = static_cast<int>(input.size() * 2 + 4096);
            std::vector<Byte> compressed(static_cast<size_t>(capacity));
            int encodedLines = 0; CompParam param{};
            const int compressedLen = comp(input.data(), compressed.data(), srcBpr, lines, capacity, 2, &encodedLines, &param, 2, nullptr);
            if (compressedLen <= 0 || compressedLen > capacity || encodedLines <= 0 || encodedLines > lines) {
                dlclose(handle); return {false, {}, "Canon SLIM compression returned an invalid result"};
            }

            // Canon's driver passes the COMPPARAM returned by lCaptCompEx
            // directly into slimCompressData. Do the same instead of using a
            // hard-coded far offset, because the compressor can change the
            // offsets between bands/images.
            std::vector<Byte> slc;
            slc.reserve(static_cast<size_t>(compressedLen) + 18);
            slc.push_back(param.xOffset[0]);
            slc.push_back(param.xOffset[1]);
            slc.push_back(param.yOffset[0]);
            slc.push_back(param.yOffset[1]);
            slc.push_back(static_cast<Byte>(param.zOffset[0]));
            slc.push_back(static_cast<Byte>(param.zOffset[1]));
            slc.push_back(static_cast<Byte>(param.farOffset & 0xff));
            slc.push_back(static_cast<Byte>((param.farOffset >> 8) & 0xff));
            slc.push_back(0x01);
            put32le(slc, compressedLen + 4);
            slc.insert(slc.end(), compressed.begin(), compressed.begin() + compressedLen);
            append(slc, {0xBD,0x3C,0xDC,0x80,0x00});

            const int transferLength = compressedLen + 18;
            if (static_cast<int>(slc.size()) != transferLength) {
                dlclose(handle); return {false, {}, "Internal Canon SLC length mismatch"};
            }
            add(transferHeader(encodedLines, transferLength)); add(slc);
        }

        add(std::vector<Byte>{0x13}); add(std::vector<Byte>{0x12}); add(std::vector<Byte>{0x11});
        std::vector<Byte> framed;
        framed.reserve(pdl.size() + pdl.size() / 4096 * 16 + 16);
        appendCmlpFrames(framed, pdl);
        dlclose(handle);

        std::ostringstream msg;
        msg << "Canon LBP6030B SFP/SLIM stream: " << framed.size() << " bytes; PDL " << pdl.size()
            << " bytes; " << width << "x" << height << " @ " << dpi << " DPI";
        return {true, std::move(framed), msg.str()};
    } catch (const std::exception& e) {
        dlclose(handle); return {false, {}, std::string("Native Canon encoder failed: ") + e.what()};
    }
}
} // namespace

extern "C" JNIEXPORT jobject JNICALL
Java_com_usbprint_app_NativeUfr2Engine_encodeNative(JNIEnv* env, jobject, jbyteArray raster, jint width, jint height, jint dpi, jint, jint) {
    if (!raster) return makeResult(env, {false, {}, "Raster is null"});
    const jsize size = env->GetArrayLength(raster), expected = ((width + 7) / 8) * height;
    if (size != expected) {
        std::ostringstream msg; msg << "Unexpected 1-bit raster size: " << size << ", expected " << expected;
        return makeResult(env, {false, {}, msg.str()});
    }
    std::vector<jbyte> pixels(static_cast<size_t>(size));
    env->GetByteArrayRegion(raster, 0, size, pixels.data());
    return makeResult(env, encode(pixels.data(), width, height, dpi));
}
