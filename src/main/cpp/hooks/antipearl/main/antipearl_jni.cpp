#include "antipearl_jni.hpp"
#include "antipearl.hpp"

JNIEXPORT void JNICALL Java_ravex_modules_combat_AntiPearl_nativePredictLanding(
    JNIEnv* env, jclass cls,
    jdouble x, jdouble y, jdouble z,
    jdouble mx, jdouble my, jdouble mz,
    jdoubleArray out) {

    ravex::Vec3 pos(x, y, z);
    ravex::Vec3 vel(mx, my, mz);

    ravex::Vec3 landing = ravex::predictPearlLanding(pos, vel);

    jdouble* elements = env->GetDoubleArrayElements(out, nullptr);
    elements[0] = landing.x;
    elements[1] = landing.y;
    elements[2] = landing.z;
    elements[3] = 0.0;
    elements[4] = 0.0;
    elements[5] = 0.0;
    env->ReleaseDoubleArrayElements(out, elements, 0);
}
