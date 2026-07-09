#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_AntiVoid_nativeIsVoidFall(
    JNIEnv* env, jclass cls,
    jdouble playerY, jdouble motionY,
    jint worldMinY, jdouble fallDistance
);

#ifdef __cplusplus
}
#endif
