#include <jni.h>
#include "bowaim.h"

extern "C" {

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_BowAim_nativeCalculateBowAim(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble targetX, jdouble targetY, jdouble targetZ,
    jdouble targetVelX, jdouble targetVelY, jdouble targetVelZ,
    jdouble targetHeight,
    jdouble arrowSpeed
) {
    ravex::BowAimResult result = ravex::solveBowAim(
        playerX, playerY, playerZ,
        targetX, targetY, targetZ,
        targetVelX, targetVelY, targetVelZ,
        targetHeight,
        arrowSpeed
    );

    jdoubleArray out = env->NewDoubleArray(4);
    if (!out) return nullptr;

    jdouble buf[4] = {
        result.hit ? 1.0 : 0.0,
        result.yaw,
        result.pitch,
        static_cast<double>(result.ticks)
    };

    env->SetDoubleArrayRegion(out, 0, 4, buf);
    return out;
}

}
