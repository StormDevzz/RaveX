#include "phase_jni.hpp"
#include "phase.hpp"

JNIEXPORT void JNICALL Java_ravex_modules_exploit_Phase_nativeCalculateOffset(
    JNIEnv* env, jclass cls,
    jdouble yaw,
    jdouble pitch,
    jdouble distance,
    jdoubleArray outOffset
) {
    double offset[3];
    calculatePhaseOffset(yaw, pitch, distance, offset);

    jdouble temp[3] = { offset[0], offset[1], offset[2] };
    env->SetDoubleArrayRegion(outOffset, 0, 3, temp);
}
