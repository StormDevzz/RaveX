#pragma once

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_render_NameTags_nativeCalculateLayout(
    JNIEnv* env, jclass cls,
    jdouble distance,
    jdouble scaleParam,
    jboolean distanceScaling,
    jboolean showArmor,
    jboolean showHands,
    jboolean hasOwner,
    jdouble tw,
    jdouble ow,
    jboolean hasMainHand,
    jboolean hasOffHand,
    jint armorCount
);

#ifdef __cplusplus
}
#endif
