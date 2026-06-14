#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jfloat JNICALL
Java_ravex_modules_movement_NoSlowDown_nativeGetBlockFriction(
    JNIEnv* env, jclass cls,
    jstring blockId,
    jfloat defaultFriction
);

#ifdef __cplusplus
}
#endif
