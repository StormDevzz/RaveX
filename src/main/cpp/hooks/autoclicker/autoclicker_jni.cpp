#include <jni.h>
#include "autoclicker.h"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jlong JNICALL
Java_ravex_modules_combat_AutoClicker_nativeCalculateDelay(
    JNIEnv* env, jclass cls,
    jdouble minCps, jdouble maxCps, jboolean randomize)
{
    return static_cast<jlong>(ravex::calculateClickDelay(minCps, maxCps, randomize == JNI_TRUE));
}

} // extern "C"
