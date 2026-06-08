#pragma once
// autocrystal_jni.h — JNI-интерфейс для AutoCrystal

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

/*
 * Class:     ravex_modules_combat_AutoCrystal
 * Method:    nativeTick
 */
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_AutoCrystal_nativeTick(
    JNIEnv* env, jclass cls,
    jdouble pX, jdouble pY, jdouble pZ,
    jdouble pHp, jdouble pAbs,
    jdoubleArray pStats,
    jdouble tX, jdouble tY, jdouble tZ,
    jdouble tHp, jdouble tAbs,
    jdoubleArray tStats,
    jdoubleArray blockData,
    jdoubleArray crystalData,
    // Конфигурация
    jdouble placeRange, jdouble breakRange,
    jdouble minTargetDmg, jdouble maxSelfDmg,
    jdouble selfDmgWeight, jboolean antiSuicide,
    jboolean armorBreaker, jdouble armorPercent,
    jdouble predictTicks, jboolean totemDetection
);

/*
 * Class:     ravex_modules_combat_AutoCrystal
 * Method:    nativeCalcDamage
 */
JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_AutoCrystal_nativeCalcDamage(
    JNIEnv* env, jclass cls,
    jdouble expX, jdouble expY, jdouble expZ,
    jdouble entityX, jdouble entityY, jdouble entityZ,
    jdouble entityHp, jdouble entityAbs,
    jdoubleArray stats,
    jdoubleArray blockData
);

#ifdef __cplusplus
}
#endif
