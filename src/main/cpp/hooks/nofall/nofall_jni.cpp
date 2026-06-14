#include "nofall_jni.h"
#include "nofall.h"
#include <string>

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_NoFall_nativeCalculateNoFall(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdouble fallDistance,
    jdouble currentY,
    jboolean currentOnGround,
    jdoubleArray outData
) {
    if (mode == nullptr) {
        return JNI_FALSE;
    }
    const char* str = env->GetStringUTFChars(mode, nullptr);
    std::string cppMode(str);
    env->ReleaseStringUTFChars(mode, str);

    bool outOnGround = currentOnGround;
    double outY = currentY;

    bool changed = calculateNoFall(cppMode, fallDistance, currentY, currentOnGround != JNI_FALSE, outOnGround, outY);

    if (changed) {
        jdouble temp[2] = { outOnGround ? 1.0 : 0.0, outY };
        env->SetDoubleArrayRegion(outData, 0, 2, temp);
        return JNI_TRUE;
    }

    return JNI_FALSE;
}
