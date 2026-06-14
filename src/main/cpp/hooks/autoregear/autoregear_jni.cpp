#include <jni.h>
#include <vector>
#include <string>
#include "autoregear.h"

extern "C" {

JNIEXPORT jint JNICALL
Java_ravex_modules_player_AutoReGear_nativeCalculateRegear(
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
    std::vector<std::string> cItemIds;
    cItemIds.reserve(containerLen);
    for (jsize i = 0; i < containerLen; ++i) {
        jstring str = (jstring)env->GetObjectArrayElement(containerItemIds, i);
        if (str) {
            const char* chars = env->GetStringUTFChars(str, nullptr);
            cItemIds.push_back(chars ? chars : "");
            env->ReleaseStringUTFChars(str, chars);
            env->DeleteLocalRef(str);
        } else {
            cItemIds.push_back("");
        }
    }

    jint* cCountsData = env->GetIntArrayElements(containerCounts, nullptr);
    jsize cCountsLen = env->GetArrayLength(containerCounts);
    std::vector<int> cCounts(cCountsData, cCountsData + cCountsLen);
    env->ReleaseIntArrayElements(containerCounts, cCountsData, JNI_ABORT);

    jsize targetLen = env->GetArrayLength(targetItemIds);
    std::vector<std::string> tItemIds;
    tItemIds.reserve(targetLen);
    for (jsize i = 0; i < targetLen; ++i) {
        jstring str = (jstring)env->GetObjectArrayElement(targetItemIds, i);
        if (str) {
            const char* chars = env->GetStringUTFChars(str, nullptr);
            tItemIds.push_back(chars ? chars : "");
            env->ReleaseStringUTFChars(str, chars);
            env->DeleteLocalRef(str);
        } else {
            tItemIds.push_back("");
        }
    }

    jint* tCountsData = env->GetIntArrayElements(targetCounts, nullptr);
    jsize tCountsLen = env->GetArrayLength(targetCounts);
    std::vector<int> tCounts(tCountsData, tCountsData + tCountsLen);
    env->ReleaseIntArrayElements(targetCounts, tCountsData, JNI_ABORT);

    jint* curCountsData = env->GetIntArrayElements(currentCounts, nullptr);
    jsize curCountsLen = env->GetArrayLength(currentCounts);
    std::vector<int> curCounts(curCountsData, curCountsData + curCountsLen);
    env->ReleaseIntArrayElements(currentCounts, curCountsData, JNI_ABORT);

    return calculateRegear(cItemIds, cCounts, tItemIds, tCounts, curCounts);
}

}
