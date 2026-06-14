#include "nonarrator_jni.h"
#include "nonarrator.h"

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoNarrator_nativeForceOff(JNIEnv* env, jclass cls) {
    forceNarratorOff();
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_NoNarrator_nativeIsForced(JNIEnv* env, jclass cls) {
    return isNarratorForced() ? JNI_TRUE : JNI_FALSE;
}
