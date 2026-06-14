#include <jni.h>
#include "voidesp.h"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_esp_VoidESP_nativeScanVoid(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble pz,
    jdouble range, jint height, jboolean floorOnly)
{
    auto data = ravex::scanVoid(px, pz, range, height, floorOnly == JNI_TRUE);
    if (data.empty()) return nullptr;

    jsize len = static_cast<jsize>(data.size());
    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, data.data());
    return result;
}

}
