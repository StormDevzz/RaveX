#include "ecfarmer_jni.hpp"
#include "ecfarmer.hpp"

JNIEXPORT jdouble JNICALL
Java_ravex_modules_player_ECFarmer_nativeCalcBreakTime(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency, jint haste,
    jint durability, jint maxDura) {

    const char* id = (toolId != nullptr) ? env->GetStringUTFChars(toolId, nullptr) : nullptr;
    ECFarmerResult result = calculateBreak(id, efficiency, haste, durability, maxDura);
    if (toolId != nullptr) env->ReleaseStringUTFChars(toolId, id);

    return result.breakTimeMs;
}

JNIEXPORT jint JNICALL
Java_ravex_modules_player_ECFarmer_nativeCalcDurabilityLoss(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency) {

    const char* id = (toolId != nullptr) ? env->GetStringUTFChars(toolId, nullptr) : nullptr;
    ECFarmerResult result = calculateBreak(id, efficiency, 0, 100, 100);
    if (toolId != nullptr) env->ReleaseStringUTFChars(toolId, id);

    return result.durabilityLoss;
}
