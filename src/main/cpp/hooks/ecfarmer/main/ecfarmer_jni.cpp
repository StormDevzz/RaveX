#include "ecfarmer_jni.hpp"
#include "ecfarmer.hpp"
#include "../../../common/include/jni_raii.hpp"

JNIEXPORT jdouble JNICALL
Java_ravex_modules_player_ECFarmer_nativeCalcBreakTime(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency, jint haste,
    jint durability, jint maxDura) {

    JniStringUtfChars idGuard(env, toolId);
    if (toolId && !idGuard) return 0.0;
    if (ravex::jni::checkAndClearException(env)) return 0.0;
    const char* id = idGuard.get();
    ECFarmerResult result = calculateBreak(id, efficiency, haste, durability, maxDura);
    if (ravex::jni::checkAndClearException(env)) return 0.0;
    return result.breakTimeMs;
}

JNIEXPORT jint JNICALL
Java_ravex_modules_player_ECFarmer_nativeCalcDurabilityLoss(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency) {

    JniStringUtfChars idGuard(env, toolId);
    if (toolId && !idGuard) return 0;
    if (ravex::jni::checkAndClearException(env)) return 0;
    const char* id = idGuard.get();
    ECFarmerResult result = calculateBreak(id, efficiency, 0, 100, 100);
    if (ravex::jni::checkAndClearException(env)) return 0;
    return result.durabilityLoss;
}
