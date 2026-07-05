#include "treecutter_jni.hpp"
#include "treecutter.hpp"

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_world_TreeCutter_nativeFindBestLog(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdoubleArray logBlocksData
) {
    std::vector<LogPos> candidates;
    if (logBlocksData) {
        jsize len = env->GetArrayLength(logBlocksData);
        jdouble* data = env->GetDoubleArrayElements(logBlocksData, nullptr);
        if (data) {
            for (jsize i = 0; i + 2 < len; i += 3) {
                candidates.push_back({ data[i], data[i+1], data[i+2] });
            }
            env->ReleaseDoubleArrayElements(logBlocksData, data, JNI_ABORT);
        }
    }

    TreeCutterResult result = findBestLog(playerX, playerY, playerZ, candidates);

    jdoubleArray out = env->NewDoubleArray(4);
    if (!out) return nullptr;

    jdouble buf[4] = {
        result.found ? 1.0 : 0.0,
        result.x,
        result.y,
        result.z
    };

    env->SetDoubleArrayRegion(out, 0, 4, buf);
    return out;
}
