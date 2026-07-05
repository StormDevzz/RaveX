#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jintArray JNICALL
Java_ravex_modules_world_Nuker_nativeFindBlocks(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range,
    jint mode,
    jintArray jBx, jintArray jBy, jintArray jBz,
    jint blockCount
);

#ifdef __cplusplus
}
#endif
