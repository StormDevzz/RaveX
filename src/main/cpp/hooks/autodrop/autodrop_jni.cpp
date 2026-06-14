#include <jni.h>
#include "autodrop.h"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_AutoDrop_nativeFindTargets(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range, jint maxResults)
{
    auto targets = ravex::findDropTargets(px, py, pz, range, maxResults);
    if (targets.empty()) return nullptr;

    jdoubleArray result = env->NewDoubleArray(targets.size() * 4);
    if (!result) return nullptr;

    std::vector<jdouble> buf(targets.size() * 4);
    for (size_t i = 0; i < targets.size(); i++) {
        buf[i * 4 + 0] = targets[i].x;
        buf[i * 4 + 1] = targets[i].y;
        buf[i * 4 + 2] = targets[i].z;
        buf[i * 4 + 3] = targets[i].score;
    }
    env->SetDoubleArrayRegion(result, 0, buf.size(), buf.data());
    return result;
}

} // extern "C"
