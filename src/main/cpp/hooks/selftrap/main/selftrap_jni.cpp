#include "selftrap_jni.hpp"
#include "selftrap.hpp"
#include "../../../common/include/jni_raii.hpp"
#include <vector>

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_SelfTrap_nativeCalculateSelfTrap(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdoubleArray solidBlockData,
    jdouble range,
    jint mode
) {
    jsize len = env->GetArrayLength(solidBlockData);
    if (ravex::jni::checkAndClearException(env)) return nullptr;

    JniDoubleElements guard(env, solidBlockData);
    if (ravex::jni::checkAndClearException(env)) return nullptr;

    std::vector<BlockPos> solidBlocks;
    if (guard) {
        const jdouble* data = guard.get();
        for (jsize i = 0; i + 2 < len; i += 3) {
            solidBlocks.push_back({ (int)data[i], (int)data[i+1], (int)data[i+2] });
        }
    }

    SelfTrapResult res = calculateSelfTrap(playerX, playerY, playerZ, solidBlocks, range, mode);

    jdoubleArray out = env->NewDoubleArray(8);
    if (!out) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;

    jdouble temp[8] = {
        res.found ? 1.0 : 0.0,
        (double)res.neighbor.x,
        (double)res.neighbor.y,
        (double)res.neighbor.z,
        (double)res.face,
        (double)res.targetBlock.x,
        (double)res.targetBlock.y,
        (double)res.targetBlock.z
    };
    env->SetDoubleArrayRegion(out, 0, 8, temp);
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return out;
}
