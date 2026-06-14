#pragma once
#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL Java_ravex_modules_exploit_Phase_nativeCalculateOffset(
    JNIEnv* env, jclass cls,
    jdouble yaw,
    jdouble pitch,
    jdouble distance,
    jdoubleArray outOffset
);

}
