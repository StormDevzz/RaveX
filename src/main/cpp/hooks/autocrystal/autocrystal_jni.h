#pragma once


#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>


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
    
    jdouble placeRange, jdouble placeWallRange,
    jdouble breakRange, jdouble breakWallRange,
    jdouble minTargetDmg, jdouble maxSelfDmg,
    jdouble selfDmgWeight, jboolean antiSuicide,
    jboolean antiSuicideCheckBreaking, jboolean antiSuicideIgnoreWithTotem,
    jboolean armorBreaker, jdouble armorPercent,
    jdouble predictTicks, jboolean totemDetection,
    jboolean totemCheckTarget, jboolean placeAirPlace,
    jboolean placeMultiPlace, jboolean suicide
);


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
