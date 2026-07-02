#pragma once
#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL Java_ravex_modules_exploit_RaytraceBypass_nativeCalculateRotation(
    JNIEnv* env, jclass cls,
    jdouble playerX,
    jdouble playerY,
    jdouble playerZ,
    jdouble blockX,
    jdouble blockY,
    jdouble blockZ,
    jdoubleArray outRotation
);

}
