#include <jni.h>
#include "witherroseaura.h"

extern "C" {

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_WitherRoseAura_nativeCalculatePlacement(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdouble range,
    jboolean targetFeetIsReplaceable,
    jboolean supportBlockIsSolid
) {
    WitherRoseResult res = calculateWitherRose(
        playerX, playerY, playerZ,
        targetX, targetY, targetZ,
        range,
        targetFeetIsReplaceable == JNI_TRUE,
        supportBlockIsSolid == JNI_TRUE
    );

    jdoubleArray out = env->NewDoubleArray(8);
    if (!out) return nullptr;

    jdouble buf[8] = {
        res.found ? 1.0 : 0.0,
        static_cast<double>(res.neighborX),
        static_cast<double>(res.neighborY),
        static_cast<double>(res.neighborZ),
        static_cast<double>(res.face),
        static_cast<double>(res.targetX),
        static_cast<double>(res.targetY),
        static_cast<double>(res.targetZ)
    };

    env->SetDoubleArrayRegion(out, 0, 8, buf);
    return out;
}

} // extern "C"
