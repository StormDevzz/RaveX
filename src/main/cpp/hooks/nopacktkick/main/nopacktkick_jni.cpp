#include "nopacktkick_jni.hpp"
#include "nopacktkick.hpp"
#include "../../../common/include/jni_raii.hpp"

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeInit(JNIEnv* env, jclass cls, jint packetsPerSec, jint burst) {
    if (ravex::jni::checkAndClearException(env)) return;
    initRateLimiter(packetsPerSec, burst);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeShouldAllow(JNIEnv* env, jclass cls) {
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return shouldAllowPacket() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeGetRate(JNIEnv* env, jclass cls) {
    if (ravex::jni::checkAndClearException(env)) return 0;
    return (jint)getCurrentRate();
}

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeReset(JNIEnv* env, jclass cls) {
    if (ravex::jni::checkAndClearException(env)) return;
    resetRateLimiter();
}
