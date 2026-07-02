#include "shieldfucker_jni.hpp"
#include "include/shieldfucker.hpp"
#include "include/shieldfucker_math.hpp"

#include <vector>
#include <cstring>

using namespace shieldfucker;

static std::vector<TargetInfo> parseEntityData(JNIEnv* env, jdoubleArray arr) {
    std::vector<TargetInfo> result;
    if (!arr) return result;
    jsize len = env->GetArrayLength(arr);
    if (len < 7) return result;

    jdouble* data = env->GetDoubleArrayElements(arr, nullptr);
    if (!data) return result;

    for (jsize i = 0; i + 6 < len; i += 7) {
        TargetInfo t;
        t.entityId     = static_cast<int>(data[i]);
        t.x            = data[i + 1];
        t.y            = data[i + 2];
        t.z            = data[i + 3];
        t.health       = data[i + 4];
        t.hasShield    = (data[i + 5] > 0.5);
        t.isBlocking   = (data[i + 6] > 0.5);
        t.distance     = 0.0;
        t.hasLineOfSight = true;
        t.isPlayer     = true;
        result.push_back(t);
    }

    env->ReleaseDoubleArrayElements(arr, data, JNI_ABORT);
    return result;
}

JNIEXPORT jobject JNICALL
Java_ravex_modules_combat_ShieldFucker_nativeTick(
    JNIEnv* env, jclass cls,
    jdouble pX, jdouble pY, jdouble pZ,
    jfloat pYaw, jfloat pPitch,
    jdoubleArray entityData,
    jdouble range, jdouble wallRange,
    jdouble switchDelay, jdouble attackDelay,
    jdouble rotateSpeed,
    jboolean throughWalls, jboolean autoSwitch,
    jboolean targetPlayers, jboolean targetMonsters,
    jboolean onlyAxe,
    jstring currentItem, jint currentSlot)
{
    Vec3 playerPos = {pX, pY, pZ};
    auto targets = parseEntityData(env, entityData);

    ShieldFuckerConfig config;
    config.range = range;
    config.wallRange = wallRange;
    config.switchDelay = switchDelay;
    config.attackDelay = attackDelay;
    config.rotateSpeed = rotateSpeed;
    config.throughWalls = (throughWalls == JNI_TRUE);
    config.autoSwitch = (autoSwitch == JNI_TRUE);
    config.targetPlayers = (targetPlayers == JNI_TRUE);
    config.targetMonsters = (targetMonsters == JNI_TRUE);
    config.onlyAxe = (onlyAxe == JNI_TRUE);

    const char* itemStr = currentItem ? env->GetStringUTFChars(currentItem, nullptr) : nullptr;

    BreakAction action = tick(
        playerPos, pYaw, pPitch,
        targets.data(), static_cast<int>(targets.size()),
        config,
        itemStr, static_cast<int>(currentSlot)
    );

    if (currentItem) env->ReleaseStringUTFChars(currentItem, itemStr);

    jclass actionClass = env->FindClass("ravex/modules/combat/ShieldFucker$BreakAction");
    if (!actionClass) return nullptr;

    jmethodID ctor = env->GetMethodID(actionClass, "<init>", "(IFFZZI)V");
    if (!ctor) return nullptr;

    return env->NewObject(actionClass, ctor,
        action.targetId,
        action.yaw,
        action.pitch,
        action.shouldBreak ? JNI_TRUE : JNI_FALSE,
        action.shouldSwitch ? JNI_TRUE : JNI_FALSE,
        action.switchSlot
    );
}

JNIEXPORT void JNICALL
Java_ravex_modules_combat_ShieldFucker_nativeReset(
    JNIEnv* env, jclass cls)
{
    (void)env;
    (void)cls;
    trackerReset();
}
