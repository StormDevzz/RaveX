#include <jni.h>
#include <vector>
#include <string>
#include "../../../common/include/jni_raii.hpp"
#include "autoregear.hpp"

extern "C" {

JNIEXPORT jint JNICALL
Java_ravex_modules_player_AutoReGear_AutoReGear_nativeCalculateRegear(
    JNIEnv* env, jclass cls,
    jobjectArray containerItemIds,
    jintArray containerCounts,
    jobjectArray targetItemIds,
    jintArray targetCounts,
    jintArray currentCounts
) {
    if (!containerItemIds || !containerCounts || !targetItemIds || !targetCounts || !currentCounts) {
        return -1;
    }

    jsize containerLen = env->GetArrayLength(containerItemIds);
    if (ravex::jni::checkAndClearException(env)) return -1;
    std::vector<std::string> cItemIds;
    cItemIds.reserve(containerLen);
    for (jsize i = 0; i < containerLen; ++i) {
        jstring str = (jstring)env->GetObjectArrayElement(containerItemIds, i);
        if (ravex::jni::checkAndClearException(env)) return -1;
        if (str) {
            JniStringUtfChars charsGuard(env, str);
            if (!charsGuard) {
                env->DeleteLocalRef(str);
                cItemIds.push_back("");
                continue;
            }
            if (ravex::jni::checkAndClearException(env)) return -1;
            cItemIds.push_back(charsGuard.get());
            env->DeleteLocalRef(str);
        } else {
            cItemIds.push_back("");
        }
    }

    JniIntElements cCountsGuard(env, containerCounts);
    if (ravex::jni::checkAndClearException(env)) return -1;
    if (!cCountsGuard) return -1;
    jsize cCountsLen = env->GetArrayLength(containerCounts);
    if (ravex::jni::checkAndClearException(env)) return -1;
    std::vector<int> cCounts(cCountsGuard.get(), cCountsGuard.get() + cCountsLen);

    jsize targetLen = env->GetArrayLength(targetItemIds);
    if (ravex::jni::checkAndClearException(env)) return -1;
    std::vector<std::string> tItemIds;
    tItemIds.reserve(targetLen);
    for (jsize i = 0; i < targetLen; ++i) {
        jstring str = (jstring)env->GetObjectArrayElement(targetItemIds, i);
        if (ravex::jni::checkAndClearException(env)) return -1;
        if (str) {
            JniStringUtfChars charsGuard(env, str);
            if (!charsGuard) {
                env->DeleteLocalRef(str);
                tItemIds.push_back("");
                continue;
            }
            if (ravex::jni::checkAndClearException(env)) return -1;
            tItemIds.push_back(charsGuard.get());
            env->DeleteLocalRef(str);
        } else {
            tItemIds.push_back("");
        }
    }

    JniIntElements tCountsGuard(env, targetCounts);
    if (ravex::jni::checkAndClearException(env)) return -1;
    if (!tCountsGuard) return -1;
    jsize tCountsLen = env->GetArrayLength(targetCounts);
    if (ravex::jni::checkAndClearException(env)) return -1;
    std::vector<int> tCounts(tCountsGuard.get(), tCountsGuard.get() + tCountsLen);

    JniIntElements curCountsGuard(env, currentCounts);
    if (ravex::jni::checkAndClearException(env)) return -1;
    if (!curCountsGuard) return -1;
    jsize curCountsLen = env->GetArrayLength(currentCounts);
    if (ravex::jni::checkAndClearException(env)) return -1;
    std::vector<int> curCounts(curCountsGuard.get(), curCountsGuard.get() + curCountsLen);

    return calculateRegear(cItemIds, cCounts, tItemIds, tCounts, curCounts);
}

}
