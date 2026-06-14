#include "antivoid_jni.h"
#include "antivoid.h"

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_AntiVoid_nativeIsVoidFall(
    JNIEnv* env, jclass cls,
    jdouble playerY, jdouble motionY,
    jint worldMinY, jdouble fallDistance
) {
    return isVoidFall(playerY, motionY, worldMinY, fallDistance) ? JNI_TRUE : JNI_FALSE;
}
