#ifndef RAVEX_PORTALBUILD_JNI_H
#define RAVEX_PORTALBUILD_JNI_H

#include <jni.h>

extern "C" {
    JNIEXPORT void JNICALL Java_ravex_modules_misc_PortalBuild_nativeFindBestPortalPos(
        JNIEnv* env, jclass cls,
        jdouble playerX, jdouble playerY, jdouble playerZ,
        jdouble playerYaw,
        jdouble minDist, jdouble maxDist,
        jdouble avoidPortalRange,
        jdoubleArray existingPortals,
        jdoubleArray groundHeights,
        jdoubleArray out);
}

#endif
