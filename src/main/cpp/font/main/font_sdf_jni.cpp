#include <jni.h>
#include <mutex>
#include "../../common/include/jni_raii.hpp"
#include "font_sdf.hpp"

static ravex::font::BakedAtlas g_atlas;
static std::mutex g_atlasMutex;

extern "C" {

JNIEXPORT jintArray JNICALL
Java_ravex_utility_render_FontSdfUtility_nativeBake(
    JNIEnv* env, jclass cls,
    jbyteArray jTtf, jint pixelHeight
) {
    if (!jTtf) return nullptr;
    jsize size = env->GetArrayLength(jTtf);
    if (ravex::jni::checkAndClearException(env)) return nullptr;

    JniByteElements guard(env, jTtf);
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    if (!guard) return nullptr;

    const jbyte* data = guard.get();
    std::lock_guard<std::mutex> lock(g_atlasMutex);
    bool ok = ravex::font::bakeAtlas(
        reinterpret_cast<unsigned char*>(const_cast<jbyte*>(data)),
        (int)size, (int)pixelHeight, g_atlas);

    if (!ok || g_atlas.metrics.empty()) return nullptr;

    jsize count = (jsize)g_atlas.metrics.size();
    jintArray out = env->NewIntArray(count);
    if (!out) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    env->SetIntArrayRegion(out, 0, count, g_atlas.metrics.data());
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return out;
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_utility_render_FontSdfUtility_nativePixels(
    JNIEnv* env, jclass cls
) {
    std::lock_guard<std::mutex> lock(g_atlasMutex);
    if (g_atlas.rgba.empty()) return nullptr;
    jsize count = (jsize)g_atlas.rgba.size();
    jbyteArray out = env->NewByteArray(count);
    if (!out) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    env->SetByteArrayRegion(out, 0, count,
        reinterpret_cast<jbyte*>(g_atlas.rgba.data()));
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return out;
}

}
