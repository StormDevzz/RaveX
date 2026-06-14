#include "replenish_jni.h"
#include "replenish.h"
#include <cstring>

JNIEXPORT jintArray JNICALL
Java_ravex_modules_player_Replenish_nativeFindReplenish(
    JNIEnv* env, jclass cls,
    jintArray jHotbarSlots,
    jobjectArray jHotbarItemIds,
    jintArray jHotbarCounts,
    jintArray jInvSlots,
    jobjectArray jInvItemIds,
    jintArray jInvCounts,
    jint jThreshold
) {
    int hbLen = env->GetArrayLength(jHotbarSlots);
    int invLen = env->GetArrayLength(jInvSlots);

    jint* hbSlots = env->GetIntArrayElements(jHotbarSlots, nullptr);
    jint* hbCounts = env->GetIntArrayElements(jHotbarCounts, nullptr);
    jint* invSlots = env->GetIntArrayElements(jInvSlots, nullptr);
    jint* invCounts = env->GetIntArrayElements(jInvCounts, nullptr);

    std::vector<InventorySlot> hotbarSlots;
    hotbarSlots.reserve(hbLen);
    for (int i = 0; i < hbLen; i++) {
        InventorySlot s;
        s.slot = hbSlots[i];
        s.count = hbCounts[i];
        jstring js = (jstring)env->GetObjectArrayElement(jHotbarItemIds, i);
        if (js) {
            const char* str = env->GetStringUTFChars(js, nullptr);
            s.itemId = str;
            env->ReleaseStringUTFChars(js, str);
        }
        hotbarSlots.push_back(s);
    }

    std::vector<InventorySlot> inventorySlots;
    inventorySlots.reserve(invLen);
    for (int i = 0; i < invLen; i++) {
        InventorySlot s;
        s.slot = invSlots[i];
        s.count = invCounts[i];
        jstring js = (jstring)env->GetObjectArrayElement(jInvItemIds, i);
        if (js) {
            const char* str = env->GetStringUTFChars(js, nullptr);
            s.itemId = str;
            env->ReleaseStringUTFChars(js, str);
        }
        inventorySlots.push_back(s);
    }

    auto results = findReplenishTargets(hotbarSlots, inventorySlots, jThreshold);

    env->ReleaseIntArrayElements(jHotbarSlots, hbSlots, JNI_ABORT);
    env->ReleaseIntArrayElements(jHotbarCounts, hbCounts, JNI_ABORT);
    env->ReleaseIntArrayElements(jInvSlots, invSlots, JNI_ABORT);
    env->ReleaseIntArrayElements(jInvCounts, invCounts, JNI_ABORT);

    jintArray jResult = env->NewIntArray(results.size() * 3);
    if (jResult == nullptr) return nullptr;

    jsize idx = 0;
    jint* buf = new jint[results.size() * 3];
    for (const auto& r : results) {
        buf[idx++] = r.hotbarSlot;
        buf[idx++] = r.inventorySlot;
        buf[idx++] = r.available;
    }
    env->SetIntArrayRegion(jResult, 0, results.size() * 3, buf);
    delete[] buf;

    return jResult;
}
