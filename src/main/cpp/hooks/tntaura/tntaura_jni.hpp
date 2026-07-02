#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>


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


JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_TntAura_nativeCalculateTntSlot(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble gapX, jdouble gapY, jdouble gapZ,
    jdoubleArray solidBlockData,
    jdouble range
);


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
