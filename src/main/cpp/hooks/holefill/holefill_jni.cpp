#include <jni.h>
#include "holefill.h"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_combat_HoleFill_nativeFindHoles(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range, jint maxResults)
{
    auto holes = ravex::findHoles(px, py, pz, range, maxResults);

    if (holes.empty()) return nullptr;

    jsize len = static_cast<jsize>(holes.size()) * 3;
    jintArray result = env->NewIntArray(len);
    if (!result) return nullptr;

    std::vector<jint> buffer(holes.size() * 3);
    for (size_t i = 0; i < holes.size(); i++) {
        buffer[i * 3 + 0] = holes[i].x;
        buffer[i * 3 + 1] = holes[i].y;
        buffer[i * 3 + 2] = holes[i].z;
    }

    env->SetIntArrayRegion(result, 0, len, buffer.data());
    return result;
}

} // extern "C"
