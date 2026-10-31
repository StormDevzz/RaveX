#include <jni.h>
#include <vector>
#include "../../../common/include/jni_raii.hpp"
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
    if (ravex::jni::checkAndClearException(env)) return -1;
    if (len == 0) return -1;

    JniIntElements xGuard(env, blockX);
    JniIntElements yGuard(env, blockY);
    JniIntElements zGuard(env, blockZ);
    if (ravex::jni::checkAndClearException(env)) return -1;
    if (!xGuard || !yGuard || !zGuard) return -1;

    const jint* xData = xGuard.get();
    const jint* yData = yGuard.get();
    const jint* zData = zGuard.get();

    std::vector<BlockCoord> candidates;
    candidates.reserve(len);
    for (jsize i = 0; i < len; ++i) {
        candidates.push_back({xData[i], yData[i], zData[i]});
    }

    return calculateClosestTarget(playerX, playerY, playerZ, candidates, range);
}

}
