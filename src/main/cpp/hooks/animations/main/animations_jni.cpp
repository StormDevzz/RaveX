#include <jni.h>
#include "animations.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jfloat JNICALL
Java_ravex_modules_esp_Animations_nativeUpdateAnimation(
    JNIEnv* env, jclass cls,
    jfloat current, jfloat speed, jfloat walkSpeed, jboolean smooth)
{
    return ravex::updateAnimation(current, speed, walkSpeed, smooth == JNI_TRUE);
}

}
