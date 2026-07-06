#pragma once
#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL Java_ravex_modules_exploit_RocketExtender_nativeCalculateBoost(
    JNIEnv* env, jclass cls,
    jdouble yaw,
    jdouble pitch,
    jdouble currentVx,
    jdouble currentVy,
    jdouble currentVz,
    jdouble boostFactor,
    jdoubleArray outVelocity
);

}
