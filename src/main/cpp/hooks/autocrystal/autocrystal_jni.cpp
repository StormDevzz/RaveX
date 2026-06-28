#include "autocrystal_jni.h"
#include "autocrystal.h"

#include <vector>
#include <cstdint>

using namespace ravex;

// ── Вспомогательные функции ────────────────────────────────────────────────

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

static std::vector<CrystalEntity> parseCrystalData(JNIEnv* env, jdoubleArray arr) {
    std::vector<CrystalEntity> result;
    if (!arr) return result;
    jsize len = env->GetArrayLength(arr);
    if (len < 4) return result;

    jdouble* data = env->GetDoubleArrayElements(arr, nullptr);
    if (!data) return result;

    for (jsize i = 0; i + 3 < len; i += 4) {
        CrystalEntity e;
        e.entityId    = static_cast<int>(data[i]);
        e.pos.x       = data[i+1];
        e.pos.y       = data[i+2];
        e.pos.z       = data[i+3];
        e.shouldBreak = false;
        result.push_back(e);
    }

    env->ReleaseDoubleArrayElements(arr, data, JNI_ABORT);
    return result;
}

static EntityStats parseStats(JNIEnv* env, jdoubleArray arr) {
    EntityStats s{};
    if (!arr) return s;
    jsize len = env->GetArrayLength(arr);
    if (len < 15) return s; // Защитная проверка на длину 15 элементов (включая тотемы)

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
    s.totemCount           = data[14]; // Тотемы

    env->ReleaseDoubleArrayElements(arr, data, JNI_ABORT);
    return s;
}

// =============================================================================
// Java_ravex_modules_combat_AutoCrystal_nativeTick
// =============================================================================
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_AutoCrystal_nativeTick(
    JNIEnv* env, jclass cls,
    jdouble pX, jdouble pY, jdouble pZ,
    jdouble pHp, jdouble pAbs,
    jdoubleArray pStats,
    jdouble tX, jdouble tY, jdouble tZ,
    jdouble tHp, jdouble tAbs,
    jdoubleArray tStats,
    jdoubleArray blockData,
    jdoubleArray crystalData,
    jdouble placeRange, jdouble placeWallRange,
    jdouble breakRange, jdouble breakWallRange,
    jdouble minTargetDmg, jdouble maxSelfDmg,
    jdouble selfDmgWeight, jboolean antiSuicide,
    jboolean antiSuicideCheckBreaking, jboolean antiSuicideIgnoreWithTotem,
    jboolean armorBreaker, jdouble armorPercent,
    jdouble predictTicks, jboolean totemDetection,
    jboolean totemCheckTarget, jboolean placeAirPlace,
    jboolean placeMultiPlace, jboolean suicide)
{
    Vec3 playerPos = {pX, pY, pZ};
    Vec3 targetPos = {tX, tY, tZ};

    auto pStatsStruct = parseStats(env, pStats);
    auto tStatsStruct = parseStats(env, tStats);

    auto blocks   = parseBlockData(env, blockData);
    auto crystals = parseCrystalData(env, crystalData);

    AutoCrystalConfig config;
    config.placeRange       = placeRange;
    config.placeWallRange   = placeWallRange;
    config.breakRange       = breakRange;
    config.breakWallRange   = breakWallRange;
    config.minTargetDamage  = minTargetDmg;
    config.maxSelfDamage    = maxSelfDmg;
    config.selfDamageWeight = selfDmgWeight;
    config.antiSuicide      = (antiSuicide == JNI_TRUE);
    config.antiSuicideCheckBreaking = (antiSuicideCheckBreaking == JNI_TRUE);
    config.antiSuicideIgnoreWithTotem = (antiSuicideIgnoreWithTotem == JNI_TRUE);
    
    // Новые настройки обхода и логики
    config.armorBreaker     = (armorBreaker == JNI_TRUE);
    config.armorPercent     = armorPercent;
    config.predictTicks     = predictTicks;
    config.totemDetection   = (totemDetection == JNI_TRUE);
    config.totemCheckTarget = (totemCheckTarget == JNI_TRUE);
    config.placeAirPlace    = (placeAirPlace == JNI_TRUE);
    config.placeMultiPlace  = (placeMultiPlace == JNI_TRUE);
    config.suicide          = (suicide == JNI_TRUE);

    AutoCrystalResult result = AutoCrystalMath::tick(
        playerPos, pHp, pAbs, pStatsStruct,
        targetPos, tHp, tAbs, tStatsStruct,
        blocks, crystals, config
    );

    jdoubleArray out = env->NewDoubleArray(18);
    if (!out) return nullptr;

    jdouble buf[18] = {
        result.shouldPlace ? 1.0 : 0.0,
        result.bestPlacement.blockPos.x,
        result.bestPlacement.blockPos.y,
        result.bestPlacement.blockPos.z,
        result.bestPlacement.targetDamage,
        result.bestPlacement.selfDamage,
        result.shouldBreak ? 1.0 : 0.0,
        (double)result.breakEntityId,
        result.breakPos.x,
        result.breakPos.y,
        result.breakPos.z,
        result.breakDamage,
        result.shouldPlace2 ? 1.0 : 0.0,
        result.secondPlacement.blockPos.x,
        result.secondPlacement.blockPos.y,
        result.secondPlacement.blockPos.z,
        result.secondPlacement.targetDamage,
        result.secondPlacement.selfDamage
    };

    env->SetDoubleArrayRegion(out, 0, 18, buf);
    return out;
}

// =============================================================================
// Java_ravex_modules_combat_AutoCrystal_nativeCalcDamage
// =============================================================================
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_AutoCrystal_nativeCalcDamage(
    JNIEnv* env, jclass cls,
    jdouble expX, jdouble expY, jdouble expZ,
    jdouble entityX, jdouble entityY, jdouble entityZ,
    jdouble entityHp, jdouble entityAbs,
    jdoubleArray stats,
    jdoubleArray blockData)
{
    auto entityStats = parseStats(env, stats);
    auto blocks = parseBlockData(env, blockData);

    Vec3 explosionPos = {expX, expY, expZ};
    Vec3 entityPos    = {entityX, entityY, entityZ};

    double dmg = AutoCrystalMath::calcExplosionDamage(
        explosionPos, entityPos, entityHp, entityAbs, entityStats, blocks
    );

    jdoubleArray out = env->NewDoubleArray(1);
    if (!out) return nullptr;

    jdouble buf[1] = { dmg };
    env->SetDoubleArrayRegion(out, 0, 1, buf);
    return out;
}
