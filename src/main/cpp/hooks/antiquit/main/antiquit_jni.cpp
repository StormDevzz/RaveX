#include "antiquit_jni.hpp"
#include "antiquit.hpp"
#include "../../../common/include/jni_raii.hpp"

JNIEXPORT void JNICALL
Java_ravex_modules_misc_AntiQuit_nativeBlockQuit(JNIEnv* env, jclass cls, jboolean block) {
    if (ravex::jni::checkAndClearException(env)) return;
    blockQuit(block == JNI_TRUE);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_AntiQuit_nativeIsQuitBlocked(JNIEnv* env, jclass cls) {
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return isQuitBlocked() ? JNI_TRUE : JNI_FALSE;
}
