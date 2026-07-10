#include "include/AddonJni.hpp"
#include "include/AddonManager.hpp"
#include <memory>
#include <vector>

static std::unique_ptr<ravex::addon::AddonManager> g_addonManager;

extern "C" {

JNIEXPORT void JNICALL Java_ravex_addon_AddonManager_nativeInitAddons(JNIEnv* env, jobject obj, jobjectArray pathArray) {
    if (!g_addonManager) {
        g_addonManager = std::make_unique<ravex::addon::AddonManager>();
    }

    if (!pathArray) return;

    jsize count = env->GetArrayLength(pathArray);
    std::vector<std::string> paths;
    paths.reserve(count);

    for (jsize i = 0; i < count; i++) {
        jstring pathStr = (jstring)env->GetObjectArrayElement(pathArray, i);
        if (!pathStr) continue;

        const char* pathChars = env->GetStringUTFChars(pathStr, nullptr);
        if (pathChars) {
            paths.push_back(std::string(pathChars));
            env->ReleaseStringUTFChars(pathStr, pathChars);
        }
        env->DeleteLocalRef(pathStr);
    }

    g_addonManager->loadPaths(paths);
}

JNIEXPORT void JNICALL Java_ravex_addon_AddonManager_nativeUnloadAddons(JNIEnv* env, jobject obj) {
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
