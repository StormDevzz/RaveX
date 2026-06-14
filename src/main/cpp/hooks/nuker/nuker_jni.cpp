#include "nuker_jni.h"
#include "nuker.h"

JNIEXPORT jintArray JNICALL
Java_ravex_modules_world_Nuker_nativeFindBlocks(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble range,
    jint mode,
    jintArray jBx, jintArray jBy, jintArray jBz,
    jint blockCount
) {
    jint* bx = env->GetIntArrayElements(jBx, nullptr);
    jint* by = env->GetIntArrayElements(jBy, nullptr);
    jint* bz = env->GetIntArrayElements(jBz, nullptr);

    std::vector<BlockPos3> candidates;
    candidates.reserve(blockCount);
    for (int i = 0; i < blockCount; i++) {
        candidates.push_back({bx[i], by[i], bz[i]});
    }

    env->ReleaseIntArrayElements(jBx, bx, JNI_ABORT);
    env->ReleaseIntArrayElements(jBy, by, JNI_ABORT);
    env->ReleaseIntArrayElements(jBz, bz, JNI_ABORT);

    Vec3d eyePos = {px, py, pz};
    auto targets = findNukerTargets(eyePos, range, (int)mode, candidates);

    jintArray jResult = env->NewIntArray(targets.size() * 3);
    if (jResult == nullptr) return nullptr;

    jsize idx = 0;
    for (const auto& t : targets) {
        jint buf[3] = {t.x, t.y, t.z};
        env->SetIntArrayRegion(jResult, idx * 3, 3, buf);
        idx++;
    }

    return jResult;
}
