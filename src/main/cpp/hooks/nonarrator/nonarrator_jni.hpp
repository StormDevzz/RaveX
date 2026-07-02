#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoNarrator_nativeForceOff(JNIEnv* env, jclass cls);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_NoNarrator_nativeIsForced(JNIEnv* env, jclass cls);

#ifdef __cplusplus
}
#endif
