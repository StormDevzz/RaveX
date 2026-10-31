#include <jni.h>
#include "../../../common/include/jni_raii.hpp"
#include "fastexp.hpp"

static ravex::FastXp fastXp;
static JavaVM* cachedJvm = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    cachedJvm = vm;
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_MiddleClick_nativeStartFastXp(
    JNIEnv* env, jclass cls)
{
    if (fastXp.isRunning()) return;
    jmethodID mid = env->GetStaticMethodID(cls, "fastXpCallback", "()V");
    if (!mid || ravex::jni::checkAndClearException(env)) {
        return;
    }
    fastXp.start(cachedJvm, cls, mid);
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_MiddleClick_nativeStopFastXp(
    JNIEnv* env, jclass)
{
    if (ravex::jni::checkAndClearException(env)) return;
    fastXp.stop(env);
}

}
