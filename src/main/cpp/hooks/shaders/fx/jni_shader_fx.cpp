#include <jni.h>
#include "../hand/include/shader_fx.hpp"

namespace {

ravex::shaders::fx::Context makeContext(jint effect, jfloat time, jint color, jint alpha) {
    ravex::shaders::fx::Context ctx;
    ctx.effect = (int)effect;
    ctx.time = (float)time;
    ctx.baseR = ((color >> 16) & 0xff) / 255.0f;
    ctx.baseG = ((color >> 8) & 0xff) / 255.0f;
    ctx.baseB = (color & 0xff) / 255.0f;
    ctx.alpha = alpha < 0 ? 0 : (alpha > 255 ? 255 : (int)alpha);
    return ctx;
}

}

extern "C" {

JNIEXPORT void JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nFxVertex(JNIEnv* env, jclass,
    jint effect, jfloat time, jint color, jint alpha,
    jfloat x, jfloat y, jfloat z,
    jfloat nx, jfloat ny, jfloat nz,
    jfloat u, jfloat v, jfloatArray out)
{
    if (!out || env->GetArrayLength(out) < 8) return;
    auto ctx = makeContext(effect, time, color, alpha);
    auto res = ravex::shaders::fx::process(ctx, x, y, z, nx, ny, nz, u, v);
    jfloat buf[8] = {
        (jfloat)res.r, (jfloat)res.g, (jfloat)res.b, (jfloat)res.a,
        res.offsetX, res.offsetY, res.offsetZ, res.glow
    };
    env->SetFloatArrayRegion(out, 0, 8, buf);
}

JNIEXPORT jint JNICALL
Java_ravex_utility_shaders_nativec_ShaderNative_nFxTint(JNIEnv*, jclass,
    jint effect, jfloat time, jint color, jint alpha, jfloat x, jfloat y, jfloat z)
{
    auto ctx = makeContext(effect, time, color, alpha);
    ctx.tint = true;
    auto res = ravex::shaders::fx::process(ctx, x, y, z, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    return (jint)((res.a << 24) | (res.r << 16) | (res.g << 8) | res.b);
}

}
