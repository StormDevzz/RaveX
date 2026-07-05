#ifndef RAVEX_BEDBOMB_JNI_H
#define RAVEX_BEDBOMB_JNI_H

#include <jni.h>

extern "C" {
    JNIEXPORT void JNICALL Java_ravex_modules_combat_BedBomb_nativeFindBestPlace(
        JNIEnv* env, jclass cls,
        jdouble px, jdouble py, jdouble pz,
        jdouble ex, jdouble ey, jdouble ez,
        jdouble range, jdoubleArray out);
}

#endif
