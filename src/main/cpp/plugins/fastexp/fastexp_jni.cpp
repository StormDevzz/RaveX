#include <jni.h>
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
    if (!mid || env->ExceptionCheck()) {
        env->ExceptionClear();
        return;
    }
    fastXp.start(cachedJvm, cls, mid);
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_MiddleClick_nativeStopFastXp(
    JNIEnv* env, jclass)
{
    fastXp.stop(env);
}

}
