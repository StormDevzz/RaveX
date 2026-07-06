#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_BasePlace_nativeCalculateBasePlace(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble playerHp, jdouble playerAbs,
    jdoubleArray playerStats,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdouble targetHp, jdouble targetAbs,
    jdoubleArray targetStats,
    jdoubleArray solidBlocksData,
    jdouble placeRange,
    jdouble targetRange,
    jdouble minTargetDmg,
    jdouble maxSelfDmg,
    jdouble selfDmgWeight,
    jboolean antiSuicide,
    jdouble antiSuicideMinHp,
    jdouble predictTicks,
    jboolean airPlace
);

#ifdef __cplusplus
}
#endif
