#include "bedbomb_jni.hpp"
#include "bedbomb.hpp"

JNIEXPORT void JNICALL Java_ravex_modules_combat_BedBomb_nativeFindBestPlace(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble ex, jdouble ey, jdouble ez,
    jdouble range, jdoubleArray out) {

    ravex::Vec3 playerPos(px, py, pz);
    ravex::Vec3 enemyPos(ex, ey, ez);

    ravex::Vec3 best = ravex::findBestBedPlace(playerPos, enemyPos, range);

    jdouble* elements = env->GetDoubleArrayElements(out, nullptr);
    elements[0] = best.x;
    elements[1] = best.y;
    elements[2] = best.z;
    elements[3] = 0.0;
    env->ReleaseDoubleArrayElements(out, elements, 0);
}
