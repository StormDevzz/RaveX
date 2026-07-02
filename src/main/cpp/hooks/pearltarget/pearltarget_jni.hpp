#pragma once
#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL Java_ravex_modules_combat_PearlTarget_nativePredictPearl(
    JNIEnv* env, jclass cls,
    jdouble x, jdouble y, jdouble z,
    jdouble mx, jdouble my, jdouble mz,
    jint maxTicks, jdoubleArray out);

JNIEXPORT void JNICALL Java_ravex_modules_combat_PearlTarget_nativeCalcIntercept(
    JNIEnv* env, jclass cls,
    jdouble fromX, jdouble fromY, jdouble fromZ,
    jdouble toX, jdouble toY, jdouble toZ,
    jdouble maxSpeed, jint maxTicks,
    jdoubleArray out);

}
