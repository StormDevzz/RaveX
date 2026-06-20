#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

/*
 * Class:     ravex_modules_combat_TntAura
 * Method:    nativeCalculateCage
 * Returns:   double[] with cage placement data
 *   [0] = found (1.0/0.0)
 *   [1] = neighborX, [2] = neighborY, [3] = neighborZ
 *   [4] = face
 *   [5] = targetBlockX, [6] = targetBlockY, [7] = targetBlockZ
 */
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
);

/*
 * Class:     ravex_modules_combat_TntAura
 * Method:    nativeCalculateTntSlot
 * Returns:   double[8] with TNT placement data
 */
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_TntAura_nativeCalculateTntSlot(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble gapX, jdouble gapY, jdouble gapZ,
    jdoubleArray solidBlockData,
    jdouble range
);

/*
 * Class:     ravex_modules_combat_TntAura
 * Method:    nativeEstimateDamage
 * Returns:   double[4] = {rawDamage, finalDamage, killProbability, lethal(1/0)}
 */
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_TntAura_nativeEstimateDamage(
    JNIEnv* env, jclass cls,
    jdouble tntX, jdouble tntY, jdouble tntZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdouble targetHealth,
    jint armorPoints, jint armorToughness,
    jint blastProtLevel,
    jboolean hasResistance, jint resistanceAmplifier
);

#ifdef __cplusplus
}
#endif
