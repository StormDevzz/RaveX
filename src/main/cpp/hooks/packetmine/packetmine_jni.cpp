#include <jni.h>
#include "packetmine.h"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_combat_PacketMine_nativeFindTargets(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range, jint maxResults, jint targetBlockId)
{
    auto targets = ravex::findMineTargets(px, py, pz, range, maxResults);
    if (targets.empty()) return nullptr;

    jsize len = static_cast<jsize>(targets.size()) * 3;
    jintArray result = env->NewIntArray(len);
    if (!result) return nullptr;

    std::vector<jint> buffer(targets.size() * 3);
    for (size_t i = 0; i < targets.size(); i++) {
        buffer[i * 3 + 0] = targets[i].x;
        buffer[i * 3 + 1] = targets[i].y;
        buffer[i * 3 + 2] = targets[i].z;
    }

    env->SetIntArrayRegion(result, 0, len, buffer.data());
    return result;
}

JNIEXPORT jlong JNICALL
Java_ravex_modules_combat_PacketMine_nativeEstimateBreakTime(
    JNIEnv* env, jclass cls,
    jint bx, jint by, jint bz,
    jdouble px, jdouble py, jdouble pz)
{
    return static_cast<jlong>(ravex::estimateBreakTime(bx, by, bz, px, py, pz));
}

} // extern "C"
