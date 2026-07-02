#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_world_TreeCutter_nativeFindBestLog(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdoubleArray logBlocksData
);

#ifdef __cplusplus
}
#endif
