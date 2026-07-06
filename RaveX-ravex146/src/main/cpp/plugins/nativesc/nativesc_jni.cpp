#include <jni.h>
#include "include/nativesc.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jboolean JNICALL
Java_ravex_nativesc_NativeScBridge_nativeInit(
    JNIEnv* env, jclass)
{
    return ravex::nativesc::initialize() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ravex_nativesc_NativeScBridge_nativeShutdown(
    JNIEnv* env, jclass)
{
    ravex::nativesc::shutdown();
}

JNIEXPORT jstring JNICALL
Java_ravex_nativesc_NativeScBridge_nativeCaptureScreen(
    JNIEnv* env, jclass, jstring path)
{
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    auto result = ravex::nativesc::captureScreen(
        ravex::nativesc::CaptureSource::FullScreen,
        ravex::nativesc::ImageFormat::PNG);
    bool saved = false;
    if (result.success && pathStr) {
        saved = ravex::nativesc::saveToFile(result, pathStr);
    }
    env->ReleaseStringUTFChars(path, pathStr);
    return env->NewStringUTF(saved ? "ok" : result.errorMsg.c_str());
}

JNIEXPORT jobjectArray JNICALL
Java_ravex_nativesc_NativeScBridge_nativeListMonitors(
    JNIEnv* env, jclass)
{
    auto monitors = ravex::nativesc::listMonitors();
    jobjectArray arr = env->NewObjectArray(
        monitors.size(), env->FindClass("java/lang/String"), nullptr);
    for (size_t i = 0; i < monitors.size(); i++) {
        env->SetObjectArrayElement(arr, i, env->NewStringUTF(monitors[i].c_str()));
    }
    return arr;
}

}
