#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jdouble JNICALL
Java_ravex_modules_player_ECFarmer_nativeCalcBreakTime(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency, jint haste,
    jint durability, jint maxDura
);

JNIEXPORT jint JNICALL
Java_ravex_modules_player_ECFarmer_nativeCalcDurabilityLoss(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency
);

#ifdef __cplusplus
}
#endif
