#include "fakepearl_jni.hpp"
#include "fakepearl.hpp"

JNIEXPORT void JNICALL
Java_ravex_modules_exploit_FakePearl_nativeCalculateVelocity(
    JNIEnv* env, jclass cls,
    jdouble yaw,
    jdouble pitch,
    jdouble speed,
    jdoubleArray outVel
) {
    double vel[3];
    calculateVelocity(yaw, pitch, speed, vel);

    jdouble temp[3] = { vel[0], vel[1], vel[2] };
    env->SetDoubleArrayRegion(outVel, 0, 3, temp);
}
