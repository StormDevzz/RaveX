#include "pearltarget_jni.hpp"
#include "pearltarget.hpp"

JNIEXPORT void JNICALL Java_ravex_modules_combat_PearlTarget_nativePredictPearl(
    JNIEnv* env, jclass cls,
    jdouble x, jdouble y, jdouble z,
    jdouble mx, jdouble my, jdouble mz,
    jint maxTicks, jdoubleArray out) {

    ravex::Vec3 pos(x, y, z);
    ravex::Vec3 vel(mx, my, mz);
    ravex::PearlPrediction result = ravex::predictPearl(pos, vel, maxTicks);

    jdouble* elements = env->GetDoubleArrayElements(out, nullptr);
    elements[0] = result.landingPos.x;
    elements[1] = result.landingPos.y;
    elements[2] = result.landingPos.z;
    elements[3] = (jdouble)result.impactTicks;
    elements[4] = result.maxHeight;
    elements[5] = result.willHitGround ? 1.0 : 0.0;
    elements[6] = result.totalDistance;
    env->ReleaseDoubleArrayElements(out, elements, 0);
}

JNIEXPORT void JNICALL Java_ravex_modules_combat_PearlTarget_nativeCalcIntercept(
    JNIEnv* env, jclass cls,
    jdouble fromX, jdouble fromY, jdouble fromZ,
    jdouble toX, jdouble toY, jdouble toZ,
    jdouble maxSpeed, jint maxTicks,
    jdoubleArray out) {

    ravex::Vec3 from(fromX, fromY, fromZ);
    ravex::Vec3 to(toX, toY, toZ);
    ravex::InterceptResult result = ravex::calcIntercept(from, to, maxSpeed, maxTicks);

    jdouble* elements = env->GetDoubleArrayElements(out, nullptr);
    elements[0] = result.velocity.x;
    elements[1] = result.velocity.y;
    elements[2] = result.velocity.z;
    elements[3] = result.requiredSpeed;
    elements[4] = (jdouble)result.estimatedTicks;
    elements[5] = result.reachable ? 1.0 : 0.0;
    env->ReleaseDoubleArrayElements(out, elements, 0);
}
