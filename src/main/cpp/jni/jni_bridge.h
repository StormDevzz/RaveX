#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_ravex_modules_misc_Optimizer_nativeOptimize(JNIEnv* env, jclass clazz, jstring mode);

JNIEXPORT jlong JNICALL
Java_ravex_modules_misc_Optimizer_nativeFreeMemory(JNIEnv* env, jclass clazz);

#ifdef __cplusplus
}
#endif
