#include "selftrap_jni.h"
#include "selftrap.h"
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
    jdouble* data = env->GetDoubleArrayElements(solidBlockData, nullptr);
    
    std::vector<BlockPos> solidBlocks;
    if (data != nullptr) {
        for (jsize i = 0; i + 2 < len; i += 3) {
            solidBlocks.push_back({ (int)data[i], (int)data[i+1], (int)data[i+2] });
        }
        env->ReleaseDoubleArrayElements(solidBlockData, data, JNI_ABORT);
    }

    SelfTrapResult res = calculateSelfTrap(playerX, playerY, playerZ, solidBlocks, range, mode);

    jdoubleArray out = env->NewDoubleArray(8);
    if (out != nullptr) {
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
    }
    return out;
}
