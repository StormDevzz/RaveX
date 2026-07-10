#include <jni.h>
#include "../hand/include/shader_math.hpp"
#include "../hand/include/shader_noise.hpp"
#include "../hand/include/shader_color.hpp"

extern "C" {

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nVec3Length(JNIEnv*, jclass, jfloat x, jfloat y, jfloat z) {
    return ravex::shaders::vec3Length({x, y, z});
}

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nVec3Dot(JNIEnv*, jclass,
    jfloat x1, jfloat y1, jfloat z1, jfloat x2, jfloat y2, jfloat z2)
{
    return ravex::shaders::vec3Dot({x1,y1,z1}, {x2,y2,z2});
}

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nVec3Normalize(JNIEnv* env, jclass, jfloatArray v) {
    jfloat buf[3];
    env->GetFloatArrayRegion(v, 0, 3, buf);
    auto n = ravex::shaders::vec3Normalize({buf[0], buf[1], buf[2]});
    buf[0] = n.x; buf[1] = n.y; buf[2] = n.z;
    env->SetFloatArrayRegion(v, 0, 3, buf);
}

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nMatrixMul(JNIEnv* env, jclass,
    jfloatArray a, jfloatArray b, jfloatArray out)
{
    jfloat ma[16], mb[16];
    env->GetFloatArrayRegion(a, 0, 16, ma);
    env->GetFloatArrayRegion(b, 0, 16, mb);

    ravex::shaders::Matrix4x4 m1, m2;
    for (int i = 0; i < 16; i++) { m1.m[i] = ma[i]; m2.m[i] = mb[i]; }

    auto res = m1 * m2;
    jfloat jres[16];
    for (int i = 0; i < 16; i++) jres[i] = res.m[i];
    env->SetFloatArrayRegion(out, 0, 16, jres);
}

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nMatrixTransform(JNIEnv* env, jclass,
    jfloatArray m, jfloat x, jfloat y, jfloat z, jfloat w, jfloatArray out)
{
    jfloat jm[16];
    env->GetFloatArrayRegion(m, 0, 16, jm);
    ravex::shaders::Matrix4x4 mat;
    for (int i = 0; i < 16; i++) mat.m[i] = jm[i];

    auto v = mat * ravex::shaders::Vec4{x, y, z, w};
    jfloat jres[4] = {v.x, v.y, v.z, v.w};
    env->SetFloatArrayRegion(out, 0, 4, jres);
}

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nPerlinNoise(JNIEnv*, jclass, jfloat x, jfloat y, jfloat z) {
    return ravex::shaders::perlinNoise(x, y, z);
}

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nFbmNoise(JNIEnv*, jclass,
    jfloat x, jfloat y, jfloat z, jint octaves, jfloat lacunarity, jfloat gain)
{
    return ravex::shaders::fbmNoise(x, y, z, octaves, lacunarity, gain);
}

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nSimplexNoise(JNIEnv*, jclass, jfloat x, jfloat y, jfloat z) {
    return ravex::shaders::simplexNoise(x, y, z);
}

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nValueNoise2D(JNIEnv*, jclass, jfloat x, jfloat y) {
    return ravex::shaders::valueNoise(x, y);
}

JNIEXPORT jfloat JNICALL
Java_ravex_shaders_nativec_ShaderNative_nCellularNoise(JNIEnv*, jclass, jfloat x, jfloat y, jfloat z) {
    return ravex::shaders::cellularNoise(x, y, z, nullptr);
}

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nHsbToRgb(JNIEnv* env, jclass,
    jfloat h, jfloat s, jfloat v, jfloatArray out)
{
    auto c = ravex::shaders::hsbToRgb({h, s, v});
    jfloat jres[3] = {c.r, c.g, c.b};
    env->SetFloatArrayRegion(out, 0, 3, jres);
}

JNIEXPORT void JNICALL
Java_ravex_shaders_nativec_ShaderNative_nRgbToHsb(JNIEnv* env, jclass,
    jfloat r, jfloat g, jfloat b, jfloatArray out)
{
    auto c = ravex::shaders::rgbToHsb({r, g, b});
    jfloat jres[3] = {c.r, c.g, c.b};
    env->SetFloatArrayRegion(out, 0, 3, jres);
}

JNIEXPORT jint JNICALL
Java_ravex_shaders_nativec_ShaderNative_nBlendColors(JNIEnv*, jclass, jint c1, jint c2, jfloat t) {
    auto a = ravex::shaders::intToRgba((int)c1);
    auto b = ravex::shaders::intToRgba((int)c2);
    auto r = ravex::shaders::blendColors(a, b, t);
    return (jint)ravex::shaders::rgbaToInt(r);
}

}
