#include <jni.h>
#include "font_sdf.hpp"

static ravex::font::BakedAtlas g_atlas;

extern "C" {

JNIEXPORT jintArray JNICALL
Java_ravex_utility_render_FontSdfUtility_nativeBake(
    JNIEnv* env, jclass cls,
    jbyteArray jTtf, jint pixelHeight
) {
    if (!jTtf) return nullptr;
    jsize size = env->GetArrayLength(jTtf);
    jbyte* data = env->GetByteArrayElements(jTtf, nullptr);
    if (!data) return nullptr;

    bool ok = ravex::font::bakeAtlas(
        reinterpret_cast<unsigned char*>(data),
        (int)size, (int)pixelHeight, g_atlas);

    env->ReleaseByteArrayElements(jTtf, data, JNI_ABORT);

    if (!ok || g_atlas.metrics.empty()) return nullptr;

    jsize count = (jsize)g_atlas.metrics.size();
    jintArray out = env->NewIntArray(count);
    if (!out) return nullptr;
    env->SetIntArrayRegion(out, 0, count, g_atlas.metrics.data());
    return out;
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_utility_render_FontSdfUtility_nativePixels(
    JNIEnv* env, jclass cls
) {
    if (g_atlas.rgba.empty()) return nullptr;
    jsize count = (jsize)g_atlas.rgba.size();
    jbyteArray out = env->NewByteArray(count);
    if (!out) return nullptr;
    env->SetByteArrayRegion(out, 0, count,
        reinterpret_cast<jbyte*>(g_atlas.rgba.data()));
    return out;
}

}
