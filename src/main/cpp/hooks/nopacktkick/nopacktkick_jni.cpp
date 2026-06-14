#include "nopacktkick_jni.h"
#include "nopacktkick.h"

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeInit(JNIEnv* env, jclass cls, jint packetsPerSec, jint burst) {
    initRateLimiter(packetsPerSec, burst);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeShouldAllow(JNIEnv* env, jclass cls) {
    return shouldAllowPacket() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeGetRate(JNIEnv* env, jclass cls) {
    return (jint)getCurrentRate();
}

JNIEXPORT void JNICALL
Java_ravex_modules_misc_NoPacketKick_nativeReset(JNIEnv* env, jclass cls) {
    resetRateLimiter();
}
