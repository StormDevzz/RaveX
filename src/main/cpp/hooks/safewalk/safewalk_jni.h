#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_SafeWalk_nativeIsNearEdge(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jintArray solidBlockData,
    jdouble threshold
);

#ifdef __cplusplus
}
#endif
