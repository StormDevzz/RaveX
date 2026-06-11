#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_SelfTrap_nativeCalculateSelfTrap(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdoubleArray solidBlockData,
    jdouble range,
    jint mode
);

#ifdef __cplusplus
}
#endif
