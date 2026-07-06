#include "trap_jni.hpp"
#include "trap.hpp"
#include <vector>

static std::vector<BlockPos> parseBlockData(JNIEnv* env, jdoubleArray arr) {
    std::vector<BlockPos> result;
    if (!arr) return result;
    jsize len = env->GetArrayLength(arr);
    if (len < 3) return result;

    jdouble* data = env->GetDoubleArrayElements(arr, nullptr);
    if (!data) return result;

    for (jsize i = 0; i + 2 < len; i += 3) {
        result.push_back({
            static_cast<int>(data[i]),
            static_cast<int>(data[i+1]),
            static_cast<int>(data[i+2])
        });
    }

    env->ReleaseDoubleArrayElements(arr, data, JNI_ABORT);
    return result;
}

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Trap_nativeCalculateTrap(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdoubleArray solidBlockData,
    jdouble range,
    jboolean roof
) {
    std::vector<BlockPos> solidBlocks = parseBlockData(env, solidBlockData);

    TrapResult result = calculateTrap(
        playerX, playerY, playerZ,
        targetX, targetY, targetZ,
        solidBlocks,
        range,
        roof == JNI_TRUE
    );

    jdoubleArray out = env->NewDoubleArray(8);
    if (!out) return nullptr;

    jdouble buf[8] = {
        result.found ? 1.0 : 0.0,
        static_cast<double>(result.neighbor.x),
        static_cast<double>(result.neighbor.y),
        static_cast<double>(result.neighbor.z),
        static_cast<double>(result.face),
        static_cast<double>(result.targetBlock.x),
        static_cast<double>(result.targetBlock.y),
        static_cast<double>(result.targetBlock.z)
    };

    env->SetDoubleArrayRegion(out, 0, 8, buf);
    return out;
}
