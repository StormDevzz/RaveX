#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

/*
 * Class:     ravex_modules_combat_Trap
 * Method:    nativeCalculateTrap
 */
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Trap_nativeCalculateTrap(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdoubleArray solidBlockData,
    jdouble range,
    jboolean roof
);

#ifdef __cplusplus
}
#endif
