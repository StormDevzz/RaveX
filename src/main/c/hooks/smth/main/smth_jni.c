#include <jni.h>
#include "smth.h"

JNIEXPORT jboolean JNICALL
Java_ravex_modules_movement_SmthCModule_nativeIsGreaterThan(
    JNIEnv* env, jclass cls, jfloat a, jfloat b
) {
    (void)cls;
    if ((*env)->ExceptionCheck(env)) return JNI_FALSE;
    return is_greater_than(a, b) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloat JNICALL
Java_ravex_modules_movement_SmthCModule_nativeAddFloats(
    JNIEnv* env, jclass cls, jfloat a, jfloat b
) {
    (void)cls;
    if ((*env)->ExceptionCheck(env)) return 0.0f;
    return add_floats(a, b);
}
