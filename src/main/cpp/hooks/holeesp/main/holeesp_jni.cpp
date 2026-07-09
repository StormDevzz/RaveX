#include <jni.h>
#include "holeesp.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_esp_HoleESP_nativeScanHoles(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range)
{
    auto data = ravex::scanHoles(px, py, pz, range);
    if (data.empty()) return nullptr;

    jsize len = static_cast<jsize>(data.size());
    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, data.data());
    return result;
}

}
