#include "nuker_jni.hpp"
#include "nuker.hpp"
#include "../../../common/include/jni_raii.hpp"

JNIEXPORT jintArray JNICALL
Java_ravex_modules_world_nuker_Nuker_nativeFindBlocks(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range,
    jint mode,
    jintArray jBx, jintArray jBy, jintArray jBz,
    jint blockCount
) {
    JniIntElements bxGuard(env, jBx);
    JniIntElements byGuard(env, jBy);
    JniIntElements bzGuard(env, jBz);
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    if (!bxGuard || !byGuard || !bzGuard) return nullptr;

    const jint* bx = bxGuard.get();
    const jint* by = byGuard.get();
    const jint* bz = bzGuard.get();

    std::vector<BlockPos3> candidates;
    candidates.reserve(blockCount);
    for (int i = 0; i < blockCount; i++) {
        candidates.push_back({bx[i], by[i], bz[i]});
    }

    Vec3d eyePos = {px, py, pz};
    auto targets = findNukerTargets(eyePos, range, (int)mode, candidates);

    jintArray jResult = env->NewIntArray(targets.size() * 3);
    if (jResult == nullptr) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;

    jsize idx = 0;
    for (const auto& t : targets) {
        jint buf[3] = {t.x, t.y, t.z};
        env->SetIntArrayRegion(jResult, idx * 3, 3, buf);
        idx++;
    }

    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return jResult;
}
