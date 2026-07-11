#include <jni.h>
#include "packetmine.hpp"

static jintArray vec3iArrayToJIntArray(JNIEnv* env, const std::vector<ravex::Vec3i>& vecs) {
    if (vecs.empty()) return nullptr;
    jsize len = static_cast<jsize>(vecs.size()) * 3;
    jintArray result = env->NewIntArray(len);
    if (!result) return nullptr;
    std::vector<jint> buf(vecs.size() * 3);
    for (size_t i = 0; i < vecs.size(); i++) {
        buf[i * 3 + 0] = vecs[i].x;
        buf[i * 3 + 1] = vecs[i].y;
        buf[i * 3 + 2] = vecs[i].z;
    }
    env->SetIntArrayRegion(result, 0, len, buf.data());
    return result;
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_player_PacketMine_nativeFindTargets(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range, jint maxResults, jint targetBlockId)
{
    auto targets = ravex::findMineTargets(px, py, pz, range, maxResults);
    std::vector<ravex::Vec3i> out(targets.size());
    for (size_t i = 0; i < targets.size(); i++) {
        out[i].x = targets[i].x;
        out[i].y = targets[i].y;
        out[i].z = targets[i].z;
    }
    return vec3iArrayToJIntArray(env, out);
}

JNIEXPORT jlong JNICALL
Java_ravex_modules_player_PacketMine_nativeEstimateBreakTime(
    JNIEnv* env, jclass cls,
    jint bx, jint by, jint bz,
    jdouble px, jdouble py, jdouble pz)
{
    return static_cast<jlong>(ravex::estimateBreakTime(bx, by, bz, px, py, pz));
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_player_PacketMine_nativeCanSee(
    JNIEnv* env, jclass cls,
    jdouble ex, jdouble ey, jdouble ez,
    jdouble tx, jdouble ty, jdouble tz,
    jintArray solidBlocks)
{
    jsize len = env->GetArrayLength(solidBlocks) / 3;
    jint* elems = env->GetIntArrayElements(solidBlocks, nullptr);
    std::vector<ravex::Vec3i> blocks(len);
    for (jsize i = 0; i < len; i++) {
        blocks[i].x = elems[i * 3 + 0];
        blocks[i].y = elems[i * 3 + 1];
        blocks[i].z = elems[i * 3 + 2];
    }
    env->ReleaseIntArrayElements(solidBlocks, elems, JNI_ABORT);
    return ravex::canSee(ex, ey, ez, tx, ty, tz, blocks);
}

JNIEXPORT jintArray JNICALL
Java_ravex_modules_player_PacketMine_nativeFilterVisible(
    JNIEnv* env, jclass cls,
    jintArray candidates,
    jintArray solidBlocks,
    jdouble ex, jdouble ey, jdouble ez)
{
    jsize candLen = env->GetArrayLength(candidates) / 3;
    jint* candElems = env->GetIntArrayElements(candidates, nullptr);
    std::vector<ravex::Vec3i> candVecs(candLen);
    for (jsize i = 0; i < candLen; i++) {
        candVecs[i].x = candElems[i * 3 + 0];
        candVecs[i].y = candElems[i * 3 + 1];
        candVecs[i].z = candElems[i * 3 + 2];
    }
    env->ReleaseIntArrayElements(candidates, candElems, JNI_ABORT);

    jsize solidLen = env->GetArrayLength(solidBlocks) / 3;
    jint* solidElems = env->GetIntArrayElements(solidBlocks, nullptr);
    std::vector<ravex::Vec3i> solidVecs(solidLen);
    for (jsize i = 0; i < solidLen; i++) {
        solidVecs[i].x = solidElems[i * 3 + 0];
        solidVecs[i].y = solidElems[i * 3 + 1];
        solidVecs[i].z = solidElems[i * 3 + 2];
    }
    env->ReleaseIntArrayElements(solidBlocks, solidElems, JNI_ABORT);

    std::vector<ravex::Vec3i> visible;
    ravex::filterVisibleBlocks(candVecs, solidVecs, ex, ey, ez, visible);
    return vec3iArrayToJIntArray(env, visible);
}

}
