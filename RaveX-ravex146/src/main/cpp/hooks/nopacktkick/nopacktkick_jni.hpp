#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeInit(JNIEnv* env, jclass cls, jint packetsPerSec, jint burst);

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeShouldAllow(JNIEnv* env, jclass cls);

JNIEXPORT jint JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeGetRate(JNIEnv* env, jclass cls);

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeReset(JNIEnv* env, jclass cls);

#ifdef __cplusplus
}
#endif
