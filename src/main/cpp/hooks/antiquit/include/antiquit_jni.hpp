#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT void JNICALL
Java_ravex_modules_misc_AntiQuit_nativeBlockQuit(JNIEnv* env, jclass cls, jboolean block);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiQuit_nativeIsQuitBlocked(JNIEnv* env, jclass cls);

#ifdef __cplusplus
}
#endif
