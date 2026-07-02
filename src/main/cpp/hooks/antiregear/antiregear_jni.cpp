#include <jni.h>
#include <vector>
#include "antiregear.hpp"

extern "C" {

JNIEXPORT jint JNICALL
Java_ravex_modules_combat_AntiReGear_nativeCalculateTarget(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jintArray blockX, jintArray blockY, jintArray blockZ,
    jdouble range
) {
    if (!blockX || !blockY || !blockZ) return -1;

    jsize len = env->GetArrayLength(blockX);
    if (len == 0) return -1;

    jint* xData = env->GetIntArrayElements(blockX, nullptr);
    jint* yData = env->GetIntArrayElements(blockY, nullptr);
    jint* zData = env->GetIntArrayElements(blockZ, nullptr);

    std::vector<BlockCoord> candidates;
    candidates.reserve(len);
    for (jsize i = 0; i < len; ++i) {
        candidates.push_back({xData[i], yData[i], zData[i]});
    }

    env->ReleaseIntArrayElements(blockX, xData, JNI_ABORT);
    env->ReleaseIntArrayElements(blockY, yData, JNI_ABORT);
    env->ReleaseIntArrayElements(blockZ, zData, JNI_ABORT);

    return calculateClosestTarget(playerX, playerY, playerZ, candidates, range);
}

}
