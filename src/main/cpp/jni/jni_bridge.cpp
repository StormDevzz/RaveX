#include "jni_bridge.h"
#include "../optimizer/optimizer.h"
#include "../common/memory.h"
#include <cstring>

JNIEXPORT jstring JNICALL
Java_ravex_modules_misc_Optimizer_nativeOptimize(JNIEnv* env, jclass, jstring mode) {
    const char* modeStr = env->GetStringUTFChars(mode, nullptr);
    auto result = ravex::Optimizer::run(std::string(modeStr));
    env->ReleaseStringUTFChars(mode, modeStr);

    return env->NewStringUTF(result.message.c_str());
}

JNIEXPORT jlong JNICALL
Java_ravex_modules_misc_Optimizer_nativeFreeMemory(JNIEnv*, jclass) {
    return static_cast<jlong>(ravex::Memory::readMemInfo().free_kb);
}
