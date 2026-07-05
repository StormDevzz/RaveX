#include "elytraplusplus_jni.hpp"
#include "elytraplusplus.hpp"
#include <string>

JNIEXPORT void JNICALL
Java_ravex_modules_movement_ElytraFly_nativeCalculateVelocity(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdouble hSpeed, jdouble vSpeed, jdouble glide,
    jdouble yaw, jdouble pitch,
    jboolean jump, jboolean sneak,
    jdoubleArray outVel
) {
    const char* str = env->GetStringUTFChars(mode, nullptr);
    std::string modeStr(str);
    env->ReleaseStringUTFChars(mode, str);

    double velocity[3];
    calculateElytraVelocity(
        modeStr,
        hSpeed, vSpeed, glide,
        yaw, pitch,
        jump == JNI_TRUE, sneak == JNI_TRUE,
        velocity
    );

    jdouble temp[3] = { velocity[0], velocity[1], velocity[2] };
    env->SetDoubleArrayRegion(outVel, 0, 3, temp);
}

JNIEXPORT void JNICALL
Java_ravex_modules_movement_ElytraFly_nativeApplyBypass(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdoubleArray motion,
    jdouble yaw, jdouble pitch,
    jboolean jump, jboolean sneak, jboolean onGround,
    jdoubleArray outMotion
) {
    const char* str = env->GetStringUTFChars(mode, nullptr);
    std::string modeStr(str);
    env->ReleaseStringUTFChars(mode, str);

    jdouble* motionData = env->GetDoubleArrayElements(motion, nullptr);
    double mx = motionData[0];
    double my = motionData[1];
    double mz = motionData[2];
    env->ReleaseDoubleArrayElements(motion, motionData, JNI_ABORT);

    double result[3];
    applyElytraBypass(
        modeStr,
        mx, my, mz,
        yaw, pitch,
        jump == JNI_TRUE, sneak == JNI_TRUE, onGround == JNI_TRUE,
        result
    );

    jdouble temp[3] = { result[0], result[1], result[2] };
    env->SetDoubleArrayRegion(outMotion, 0, 3, temp);
}
