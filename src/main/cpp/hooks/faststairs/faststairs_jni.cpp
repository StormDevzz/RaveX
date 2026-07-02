#include "faststairs_jni.hpp"
#include "faststairs.hpp"
#include <string>

JNIEXPORT jdouble JNICALL
Java_ravex_modules_movement_FastStairs_nativeCalculateClimbSpeed(
    JNIEnv* env, jclass cls,
    jstring mode,
    jdouble currentY,
    jdouble speedFactor
) {
    if (mode == nullptr) {
        return currentY;
    }
    const char* str = env->GetStringUTFChars(mode, nullptr);
    std::string cppMode(str);
    env->ReleaseStringUTFChars(mode, str);

    return calculateClimbSpeed(cppMode, currentY, speedFactor);
}
