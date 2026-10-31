#include "bedbomb_jni.hpp"
#include "bedbomb.hpp"
#include "../../../common/include/jni_raii.hpp"

JNIEXPORT void JNICALL Java_ravex_modules_combat_BedBomb_nativeFindBestPlace(
    JNIEnv* env, jclass cls,
    jdouble px, jdouble py, jdouble pz,
    jdouble ex, jdouble ey, jdouble ez,
    jdouble range, jdoubleArray out) {

    ravex::Vec3 playerPos(px, py, pz);
    ravex::Vec3 enemyPos(ex, ey, ez);

    ravex::Vec3 best = ravex::findBestBedPlace(playerPos, enemyPos, range);

    JniDoubleElements guard(env, out);
    if (ravex::jni::checkAndClearException(env)) return;
    if (!guard) return;

    jdouble* elements = guard.get();
    elements[0] = best.x;
    elements[1] = best.y;
    elements[2] = best.z;
    elements[3] = 0.0;
}
