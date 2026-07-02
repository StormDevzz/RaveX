#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_NoFall_nativeCalculateNoFall(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdouble fallDistance,
    jdouble currentY,
    jboolean currentOnGround,
    jdoubleArray outData
);

#ifdef __cplusplus
}
#endif
