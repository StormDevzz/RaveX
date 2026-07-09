#include "include/AddonJni.hpp"
#include "include/AddonManager.hpp"
#include <memory>

static std::unique_ptr<ravex::addon::AddonManager> g_addonManager;

extern "C" {

JNIEXPORT void JNICALL Java_ravex_addon_AddonManager_nativeInitAddons(JNIEnv* env, jobject obj, jstring dirPath) {
    const char* pathChars = env->GetStringUTFChars(dirPath, nullptr);
    if (pathChars) {
        if (!g_addonManager) {
            g_addonManager = std::make_unique<ravex::addon::AddonManager>();
        }
        g_addonManager->scanAndLoad(pathChars);
        env->ReleaseStringUTFChars(dirPath, pathChars);
    }
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
