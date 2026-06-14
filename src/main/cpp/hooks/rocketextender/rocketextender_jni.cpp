#include "rocketextender_jni.h"
#include "rocketextender.h"

JNIEXPORT void JNICALL Java_ravex_modules_exploit_RocketExtender_nativeCalculateBoost(
    JNIEnv* env, jclass cls,
    jdouble yaw,
    jdouble pitch,
    jdouble currentVx,
    jdouble currentVy,
    jdouble currentVz,
    jdouble boostFactor,
    jdoubleArray outVelocity
) {
    double velocity[3];
    calculateRocketBoost(yaw, pitch, currentVx, currentVy, currentVz, boostFactor, velocity);

    jdouble temp[3] = { velocity[0], velocity[1], velocity[2] };
    env->SetDoubleArrayRegion(outVelocity, 0, 3, temp);
}
