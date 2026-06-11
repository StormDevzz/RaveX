#include <jni.h>
#include "breaker.h"
#include <vector>

using namespace ravex;

static std::vector<Vec3> parseBlockData(JNIEnv* env, jdoubleArray arr) {
    std::vector<Vec3> result;
    if (!arr) return result;
    jsize len = env->GetArrayLength(arr);
    if (len < 3) return result;
    jdouble* data = env->GetDoubleArrayElements(arr, nullptr);
    if (!data) return result;
    for (jsize i = 0; i + 2 < len; i += 3) {
        result.push_back({data[i], data[i+1], data[i+2]});
    }
    env->ReleaseDoubleArrayElements(arr, data, JNI_ABORT);
    return result;
}

static EntityStats parseStats(JNIEnv* env, jdoubleArray arr) {
    EntityStats s{};
    if (!arr) return s;
    jsize len = env->GetArrayLength(arr);
    if (len < 15) return s;
    jdouble* data = env->GetDoubleArrayElements(arr, nullptr);
    if (!data) return s;
    s.armorValue           = data[0];
    s.toughness            = data[1];
    s.blastProtectionEpf   = data[2];
    s.protectionEpf        = data[3];
    s.resistanceLevel      = data[4];
    s.weaknessLevel        = data[5];
    s.strengthLevel        = data[6];
    s.helmetDurability     = data[7];
    s.chestplateDurability = data[8];
    s.leggingsDurability   = data[9];
    s.bootsDurability      = data[10];
    s.motionX              = data[11];
    s.motionY              = data[12];
    s.motionZ              = data[13];
    s.totemCount           = data[14];
    env->ReleaseDoubleArrayElements(arr, data, JNI_ABORT);
    return s;
}

extern "C" {

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Breaker_nativeCalculateBreaker(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble playerHp, jdouble playerAbs, jdoubleArray playerStats,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdouble targetHp, jdouble targetAbs, jdoubleArray targetStats,
    jdoubleArray solidBlocksData,
    jdoubleArray breakableCandidatesData,
    jdouble breakRange,
    jdouble crystalPlaceRange,
    jdouble minTargetDmg,
    jdouble maxSelfDmg,
    jdouble selfDmgWeight,
    jboolean antiSuicide,
    jdouble antiSuicideMinHp
) {
    Vec3 pPos = {playerX, playerY, playerZ};
    Vec3 tPos = {targetX, targetY, targetZ};

    EntityStats pStats = parseStats(env, playerStats);
    EntityStats tStats = parseStats(env, targetStats);

    std::vector<Vec3> solidBlocks = parseBlockData(env, solidBlocksData);
    std::vector<Vec3> candidates = parseBlockData(env, breakableCandidatesData);

    BreakResult result = findBestBreakBlock(
        pPos, playerHp, playerAbs, pStats,
        tPos, targetHp, targetAbs, tStats,
        solidBlocks,
        candidates,
        breakRange,
        crystalPlaceRange,
        minTargetDmg,
        maxSelfDmg,
        selfDmgWeight,
        antiSuicide == JNI_TRUE,
        antiSuicideMinHp
    );

    jdoubleArray out = env->NewDoubleArray(9);
    if (!out) return nullptr;

    jdouble buf[9] = {
        result.valid ? 1.0 : 0.0,
        result.breakBlock.x,
        result.breakBlock.y,
        result.breakBlock.z,
        result.targetDamage,
        result.selfDamage,
        result.crystalPos.x,
        result.crystalPos.y,
        result.crystalPos.z
    };

    env->SetDoubleArrayRegion(out, 0, 9, buf);
    return out;
}

}
