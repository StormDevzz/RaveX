#ifndef RAVEX_ANTIPEARC_JNI_H
#define RAVEX_ANTIPEARC_JNI_H

#include <jni.h>

extern "C" {
    JNIEXPORT void JNICALL Java_ravex_modules_combat_AntiPearl_nativePredictLanding(
        JNIEnv* env, jclass cls,
        jdouble x, jdouble y, jdouble z,
        jdouble mx, jdouble my, jdouble mz,
        jdoubleArray out);
}

#endif
