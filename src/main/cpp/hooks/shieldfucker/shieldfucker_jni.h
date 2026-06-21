#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jobject JNICALL
Java_ravex_modules_combat_ShieldFucker_nativeTick(
    JNIEnv* env, jclass cls,
    jdouble pX, jdouble pY, jdouble pZ,
    jfloat pYaw, jfloat pPitch,
    jdoubleArray entityData,
    jdouble range, jdouble wallRange,
    jdouble switchDelay, jdouble attackDelay,
    jdouble rotateSpeed,
    jboolean throughWalls, jboolean autoSwitch,
    jboolean targetPlayers, jboolean targetMonsters,
    jboolean onlyAxe,
    jstring currentItem, jint currentSlot
);

JNIEXPORT void JNICALL
Java_ravex_modules_combat_ShieldFucker_nativeReset(
    JNIEnv* env, jclass cls
);

#ifdef __cplusplus
}
#endif
