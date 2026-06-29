#include "tntaura_jni.h"
#include "tntaura.h"
#include "damage_calc.h"
#include <vector>



static std::vector<TntBlockPos> parseBlockData(JNIEnv* env, jdoubleArray arr) {
    std::vector<TntBlockPos> result;
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
Java_ravex_modules_combat_TntAura_nativeCalculateCage(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdoubleArray solidBlockData,
    jdouble range,
    jboolean roof,
    jint gapDirection,
    jdoubleArray gapPosData
) {
    auto solidBlocks = parseBlockData(env, solidBlockData);

    
    TntBlockPos gapPos = {0, 0, 0};
    bool hasGap = false;
    if (gapPosData) {
        jsize gapLen = env->GetArrayLength(gapPosData);
        if (gapLen >= 3) {
            jdouble* gapData = env->GetDoubleArrayElements(gapPosData, nullptr);
            if (gapData) {
                gapPos = {(int)gapData[0], (int)gapData[1], (int)gapData[2]};
                hasGap = true;
                env->ReleaseDoubleArrayElements(gapPosData, gapData, JNI_ABORT);
            }
        }
    }

    TntAuraConfig config;
    config.range = range;
    config.roof = (roof == JNI_TRUE);
    config.includeBottom = false;
    config.gapDirection = gapDirection;

    TntPlacement placement;
    if (hasGap) {
        placement = findNextCagePlacement(
            playerX, playerY, playerZ,
            targetX, targetY, targetZ,
            solidBlocks, config, gapPos
        );
    } else {
        TntAuraResult full = calculateTntAuraCage(
            playerX, playerY, playerZ,
            targetX, targetY, targetZ,
            solidBlocks, config
        );
        if (!full.cagePlacements.empty()) {
            placement = full.cagePlacements[0];
        } else {
            placement = {false, {0,0,0}, 0, {0,0,0}};
        }
        
        jdoubleArray out = env->NewDoubleArray(11);
        if (!out) return nullptr;
        jdouble buf[11] = {
            placement.valid ? 1.0 : 0.0,
            (double)placement.neighbor.x, (double)placement.neighbor.y, (double)placement.neighbor.z,
            (double)placement.face,
            (double)placement.targetBlock.x, (double)placement.targetBlock.y, (double)placement.targetBlock.z,
            (double)full.gapPos.x, (double)full.gapPos.y, (double)full.gapPos.z
        };
        env->SetDoubleArrayRegion(out, 0, 11, buf);
        return out;
    }

    jdoubleArray out = env->NewDoubleArray(8);
    if (!out) return nullptr;
    jdouble buf[8] = {
        placement.valid ? 1.0 : 0.0,
        (double)placement.neighbor.x, (double)placement.neighbor.y, (double)placement.neighbor.z,
        (double)placement.face,
        (double)placement.targetBlock.x, (double)placement.targetBlock.y, (double)placement.targetBlock.z
    };
    env->SetDoubleArrayRegion(out, 0, 8, buf);
    return out;
}



JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_TntAura_nativeCalculateTntSlot(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble gapX, jdouble gapY, jdouble gapZ,
    jdoubleArray solidBlockData,
    jdouble range
) {
    auto solidBlocks = parseBlockData(env, solidBlockData);
    TntBlockPos gapPos = {(int)gapX, (int)gapY, (int)gapZ};

    TntPlacement p = calculateTntPlacement(
        playerX, playerY, playerZ,
        gapPos, solidBlocks, range
    );

    jdoubleArray out = env->NewDoubleArray(8);
    if (!out) return nullptr;
    jdouble buf[8] = {
        p.valid ? 1.0 : 0.0,
        (double)p.neighbor.x, (double)p.neighbor.y, (double)p.neighbor.z,
        (double)p.face,
        (double)p.targetBlock.x, (double)p.targetBlock.y, (double)p.targetBlock.z
    };
    env->SetDoubleArrayRegion(out, 0, 8, buf);
    return out;
}



JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_TntAura_nativeEstimateDamage(
    JNIEnv* env, jclass cls,
    jdouble tntX, jdouble tntY, jdouble tntZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdouble targetHealth,
    jint armorPoints, jint armorToughness,
    jint blastProtLevel,
    jboolean hasResistance, jint resistanceAmplifier
) {
    TntDamageConfig config;
    config.tntX = tntX; config.tntY = tntY; config.tntZ = tntZ;
    config.targetX = targetX; config.targetY = targetY; config.targetZ = targetZ;
    config.targetHealth = targetHealth;
    config.armorPoints = armorPoints;
    config.armorToughness = armorToughness;
    config.blastProtLevel = blastProtLevel;
    config.hasResistance = (hasResistance == JNI_TRUE);
    config.resistanceAmplifier = resistanceAmplifier;

    TntDamageResult res = calculateTntDamage(config);

    jdoubleArray out = env->NewDoubleArray(4);
    if (!out) return nullptr;
    jdouble buf[4] = {
        res.rawDamage,
        res.finalDamage,
        res.killProbability,
        res.lethal ? 1.0 : 0.0
    };
    env->SetDoubleArrayRegion(out, 0, 4, buf);
    return out;
}
