#include "raytracebypass_jni.h"
#include "raytracebypass.h"

JNIEXPORT void JNICALL Java_ravex_modules_exploit_RaytraceBypass_nativeCalculateRotation(
    JNIEnv* env, jclass cls,
    jdouble playerX,
    jdouble playerY,
    jdouble playerZ,
    jdouble blockX,
    jdouble blockY,
    jdouble blockZ,
    jdoubleArray outRotation
) {
    double rotation[2];
    calculateBypassRotation(playerX, playerY, playerZ, blockX, blockY, blockZ, rotation);

    jdouble temp[2] = { rotation[0], rotation[1] };
    env->SetDoubleArrayRegion(outRotation, 0, 2, temp);
}
