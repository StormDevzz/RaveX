#include <jni.h>
#include <cstring>
#include "../../../common/include/jni_raii.hpp"
#include "antibot.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_AntiBot_nativeAnalyze(
    JNIEnv* env, jclass cls,
    jstring jName, jint ticks,
    jdouble x, jdouble y, jdouble z,
    jdouble mx, jdouble my, jdouble mz, jdouble dist,
    jboolean pingCheck, jboolean nameCheck, jboolean moveCheck)
{
    JniStringUtfChars nameGuard(env, jName);
    if (!nameGuard) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    std::string name(nameGuard.get());

    double confidence = ravex::analyze(name, ticks, x, y, z, mx, my, mz, dist,
                                       pingCheck == JNI_TRUE,
                                       nameCheck == JNI_TRUE,
                                       moveCheck == JNI_TRUE);

    jdoubleArray result = env->NewDoubleArray(1);
    if (!result) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    jdouble val = confidence;
    env->SetDoubleArrayRegion(result, 0, 1, &val);
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return result;
}

}
