#include "safewalk_jni.hpp"
#include "safewalk.hpp"

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_SafeWalk_nativeIsNearEdge(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jintArray solidBlockData,
    jdouble threshold
) {
    jsize len = env->GetArrayLength(solidBlockData);
    jint* data = env->GetIntArrayElements(solidBlockData, nullptr);
    bool result = false;

    if (data != nullptr) {
        result = isNearEdge(playerX, playerY, playerZ, data, len, threshold);
        env->ReleaseIntArrayElements(solidBlockData, data, JNI_ABORT);
    }

    return result ? JNI_TRUE : JNI_FALSE;
}
