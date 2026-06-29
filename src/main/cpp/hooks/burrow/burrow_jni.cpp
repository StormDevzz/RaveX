#include <jni.h>
#include "burrow.h"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_Burrow_nativeCalculate(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble height, jboolean autoCenter)
{
    auto result = ravex::calculateBurrow(px, py, pz, height, autoCenter == JNI_TRUE);
    jdoubleArray arr = env->NewDoubleArray(4);
    if (!arr) return nullptr;
    jdouble buf[] = {result.targetX, result.targetZ, result.liftY, result.angle};
    env->SetDoubleArrayRegion(arr, 0, 4, buf);
    return arr;
}

} 
