#include "autoapple_jni.h"
#include "autoapple.h"

JNIEXPORT jboolean JNICALL
Java_ravex_modules_combat_AutoApple_nativeShouldEat(
    JNIEnv* env, jclass cls,
    jdouble health,
    jdouble absorption,
    jdouble healthThreshold
) {
    bool result = calculateShouldEat(
        health,
        absorption,
        healthThreshold
    );

    return result ? JNI_TRUE : JNI_FALSE;
}
