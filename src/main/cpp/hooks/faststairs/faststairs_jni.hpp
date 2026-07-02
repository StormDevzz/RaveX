#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jdouble JNICALL
Java_ravex_modules_movement_FastStairs_nativeCalculateClimbSpeed(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdouble currentY,
    jdouble speedFactor
);

#ifdef __cplusplus
}
#endif
