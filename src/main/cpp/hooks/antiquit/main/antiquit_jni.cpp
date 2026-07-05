#include "antiquit_jni.hpp"
#include "antiquit.hpp"

JNIEXPORT void JNICALL
Java_ravex_modules_misc_AntiQuit_nativeBlockQuit(JNIEnv* env, jclass cls, jboolean block) {
    blockQuit(block == JNI_TRUE);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiQuit_nativeIsQuitBlocked(JNIEnv* env, jclass cls) {
    return isQuitBlocked() ? JNI_TRUE : JNI_FALSE;
}
