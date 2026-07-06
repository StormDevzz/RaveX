#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jboolean JNICALL
Java_ravex_modules_combat_AutoApple_nativeShouldEat(
    JNIEnv* env, jclass cls,
    jdouble health,
    jdouble absorption,
    jdouble healthThreshold
);

#ifdef __cplusplus
}
#endif
