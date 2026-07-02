#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT void JNICALL
Java_ravex_modules_exploit_FakePearl_nativeCalculateVelocity(
    JNIEnv* env, jclass cls,
    jdouble yaw,
    jdouble pitch,
    jdouble speed,
    jdoubleArray outVel
);

#ifdef __cplusplus
}
#endif
