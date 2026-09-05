#include <jni.h>

namespace {

jobject makeResult(JNIEnv* env, bool success, const char* message) {
    jclass resultClass = env->FindClass("com/usbprint/app/Ufr2Encoder$Result");
    if (resultClass == nullptr) return nullptr;

    jmethodID constructor = env->GetMethodID(
        resultClass,
        "<init>",
        "(Z[BLjava/lang/String;)V"
    );
    if (constructor == nullptr) return nullptr;

    jstring text = env->NewStringUTF(message);
    jobject result = env->NewObject(
        resultClass,
        constructor,
        static_cast<jboolean>(success),
        nullptr,
        text
    );
    env->DeleteLocalRef(text);
    return result;
}

}  // namespace

extern "C"
JNIEXPORT jobject JNICALL
Java_com_usbprint_app_NativeUfr2Engine_encodeNative(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray /* raster */,
    jint /* width */,
    jint /* height */,
    jint /* dpi */,
    jint /* paperWidthMm */,
    jint /* paperHeightMm */) {
    // Deliberately fail closed. This native target proves the Android/ARM64
    // JNI boundary, but it does not pretend to implement Canon's SFP/UFR II LT
    // wire format. Real printer bytes must only be returned after the encoder
    // has been validated against an LBP6030B-compatible driver output.
    return makeResult(
        env,
        false,
        "Native UFR II LT encoder boundary loaded; SFP encoder is not implemented yet."
    );
}
