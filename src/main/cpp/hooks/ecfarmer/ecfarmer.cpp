#include <jni.h>
#include <cmath>
#include <cstring>

struct ToolInfo {
    bool hasSilkTouch;
    int efficiencyLevel;
    int hasteLevel;
    float destroySpeed;
    int currentDurability;
    int maxDurability;
    bool isValidTool;
};

static ToolInfo analyzeTool(JNIEnv* env, jstring toolId, jint efficiency, jint haste, jint durability, jint maxDura) {
    ToolInfo info;
    info.efficiencyLevel = efficiency;
    info.hasteLevel = haste;
    info.currentDurability = durability;
    info.maxDurability = maxDura;
    info.isValidTool = false;
    info.hasSilkTouch = false;
    info.destroySpeed = 1.0f;

    if (toolId == nullptr) return info;

    const char* id = env->GetStringUTFChars(toolId, nullptr);
    if (id == nullptr) return info;

    bool isPickaxe = (strstr(id, "_pickaxe") != nullptr);

    if (!isPickaxe) {
        env->ReleaseStringUTFChars(toolId, id);
        return info;
    }

    info.isValidTool = true;

    if (strstr(id, "netherite")) {
        info.destroySpeed = 9.0f;
    } else if (strstr(id, "diamond")) {
        info.destroySpeed = 8.0f;
    } else if (strstr(id, "iron")) {
        info.destroySpeed = 6.0f;
    } else if (strstr(id, "stone")) {
        info.destroySpeed = 4.0f;
    } else {
        info.destroySpeed = 2.0f;
    }

    if (info.efficiencyLevel > 0) {
        info.destroySpeed += info.efficiencyLevel * info.efficiencyLevel + 1.0f;
    }

    if (info.hasteLevel > 0) {
        info.destroySpeed *= (1.0f + info.hasteLevel * 0.2f);
    }

    env->ReleaseStringUTFChars(toolId, id);
    return info;
}

extern "C" {

JNIEXPORT jdouble JNICALL
Java_ravex_modules_world_ECFarmer_nativeCalcBreakTime(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency, jint haste,
    jint durability, jint maxDura) {

    ToolInfo info = analyzeTool(env, toolId, efficiency, haste, durability, maxDura);
    if (!info.isValidTool) return -1.0;

    double breakTime = 22.5 / info.destroySpeed / 30.0;
    return (breakTime * 1000.0) + 50.0;
}

JNIEXPORT jint JNICALL
Java_ravex_modules_world_ECFarmer_nativeCalcDurabilityLoss(
    JNIEnv* env, jclass cls,
    jstring toolId, jint efficiency) {

    ToolInfo info = analyzeTool(env, toolId, efficiency, 0, 100, 100);
    if (!info.isValidTool) return -1;

    return 1;
}

} // extern "C"
