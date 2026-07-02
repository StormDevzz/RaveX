#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_CoordLogger_nativeEnsureDir(JNIEnv* env, jclass cls, jstring path);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_CoordLogger_nativeWriteLog(JNIEnv* env, jclass cls, jstring filePath, jstring content);

#ifdef __cplusplus
}
#endif
