#include "include/AddonJni.hpp"
#include "include/AddonManager.hpp"
#include "../common/include/jni_raii.hpp"
#include <memory>
#include <vector>

static std::unique_ptr<ravex::addon::AddonManager> g_addonManager;

extern "C" {

JNIEXPORT void JNICALL Java_ravex_manager_AddonManager_nativeInitAddons(JNIEnv* env, jobject obj, jobjectArray pathArray) {
    if (!g_addonManager) {
        g_addonManager = std::make_unique<ravex::addon::AddonManager>();
    }

    if (!pathArray) return;

    jsize count = env->GetArrayLength(pathArray);
    if (ravex::jni::checkAndClearException(env)) return;
    std::vector<std::string> paths;
    paths.reserve(count);

    for (jsize i = 0; i < count; i++) {
        jstring pathStr = (jstring)env->GetObjectArrayElement(pathArray, i);
        if (ravex::jni::checkAndClearException(env)) return;
        if (!pathStr) continue;

        JniStringUtfChars pathCharsGuard(env, pathStr);
        if (!pathCharsGuard) {
            env->DeleteLocalRef(pathStr);
            continue;
        }
        if (ravex::jni::checkAndClearException(env)) return;
        paths.push_back(std::string(pathCharsGuard.get()));
        env->DeleteLocalRef(pathStr);
    }

    g_addonManager->loadPaths(paths);
}

JNIEXPORT void JNICALL Java_ravex_manager_AddonManager_nativeUnloadAddons(JNIEnv* env, jobject obj) {
    if (g_addonManager) {
        g_addonManager->unloadAll();
        g_addonManager.reset();
    }
}

}

namespace ravex {
namespace addon {
void AddonJni::registerNatives(JNIEnv* env) {

}
}
}
