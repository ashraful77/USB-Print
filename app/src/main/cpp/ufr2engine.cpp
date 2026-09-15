#include <jni.h>

namespace {
jobject failure(JNIEnv* env) {
    jclass resultClass = env->FindClass("com/usbprint/app/Ufr2Encoder$Result");
    if (!resultClass) return nullptr;

    jmethodID ctor = env->GetMethodID(
        resultClass,
        "<init>",
        "(Z[BLjava/lang/String;)V"
    );
    if (!ctor) return nullptr;

    jstring message = env->NewStringUTF(
        "No redistributable UFR II LT encoder is available; no printer data was generated."
    );
    jobject result = env->NewObject(
        resultClass,
        ctor,
        JNI_FALSE,
        nullptr,
        message
    );
    env->DeleteLocalRef(message);
    return result;
}
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_usbprint_app_NativeUfr2Engine_encodeNative(
    JNIEnv* env,
    jobject,
    jbyteArray,
    jint,
    jint,
    jint,
    jint,
    jint
) {
    return failure(env);
}
