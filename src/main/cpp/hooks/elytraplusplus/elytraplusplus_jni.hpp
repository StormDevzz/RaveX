#pragma once

#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL
Java_ravex_modules_movement_ElytraFly_nativeCalculateVelocity(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdouble hSpeed, jdouble vSpeed, jdouble glide,
    jdouble yaw, jdouble pitch,
    jboolean jump, jboolean sneak,
    jdoubleArray outVel
);

JNIEXPORT void JNICALL
Java_ravex_modules_movement_ElytraFly_nativeApplyBypass(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdoubleArray motion,
    jdouble yaw, jdouble pitch,
    jboolean jump, jboolean sneak, jboolean onGround,
    jdoubleArray outMotion
);

}
