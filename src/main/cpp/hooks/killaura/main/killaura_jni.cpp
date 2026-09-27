#include <jni.h>
#include "killaura.hpp"

extern "C" {

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_combat_KillAura_nativeCritTiming(
    JNIEnv* env, jclass,
    jdouble fall, jdouble chargeTrue, jdouble minFall)
{
    ravex::CritWindow w = ravex::evaluateCritWindow(
        static_cast<double>(fall),
        static_cast<double>(chargeTrue),
        static_cast<double>(minFall));
    jdoubleArray out = env->NewDoubleArray(3);
    if (!out) return nullptr;
    jdouble vals[3] = {w.fire ? 1.0 : 0.0, w.quality, w.serverFall};
    env->SetDoubleArrayRegion(out, 0, 3, vals);
    return out;
}

}
