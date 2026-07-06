#include <jni.h>
#include "selffill.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jdouble JNICALL
Java_ravex_modules_combat_SelfFill_nativeGetAngle(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble bx, jdouble by, jdouble bz)
{
    return ravex::calcPlaceAngle(px, pz, bx, bz);
}

} 
