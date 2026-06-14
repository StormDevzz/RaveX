#include "noslow_jni.h"
#include "noslow.h"
#include <string>

JNIEXPORT jfloat JNICALL
Java_ravex_modules_movement_NoSlowDown_nativeGetBlockFriction(
    JNIEnv* env, jclass cls,
    jstring blockId,
    jfloat defaultFriction
) {
    if (blockId == nullptr) {
        return defaultFriction;
    }
    const char* str = env->GetStringUTFChars(blockId, nullptr);
    std::string cppBlockId(str);
    env->ReleaseStringUTFChars(blockId, str);

    return getBypassFriction(cppBlockId, defaultFriction);
}
