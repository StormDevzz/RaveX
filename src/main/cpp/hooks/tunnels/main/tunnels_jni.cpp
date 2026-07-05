#include <jni.h>
#include "tunnels.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_esp_Tunnels_nativeScanTunnels(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range, jint maxY, jint minY)
{
    auto data = ravex::scanTunnels(px, py, pz, range, maxY, minY);
    if (data.empty()) return nullptr;

    jsize len = static_cast<jsize>(data.size());
    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, data.data());
    return result;
}

}
