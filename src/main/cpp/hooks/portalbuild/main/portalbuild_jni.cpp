#include "portalbuild_jni.hpp"
#include "portalbuild.hpp"

JNIEXPORT void JNICALL Java_ravex_modules_misc_PortalBuild_nativeFindBestPortalPos(
    JNIEnv* env, jclass cls,
    jdouble playerX, jdouble playerY, jdouble playerZ,
    jdouble playerYaw,
    jdouble minDist, jdouble maxDist,
    jdouble avoidPortalRange,
    jdoubleArray existingPortals,
    jdoubleArray groundHeights,
    jdoubleArray out) {

    jsize portalCount = existingPortals ? env->GetArrayLength(existingPortals) / 3 : 0;
    jdouble* portalData = portalCount > 0 ? env->GetDoubleArrayElements(existingPortals, nullptr) : nullptr;

    jsize groundCount = groundHeights ? env->GetArrayLength(groundHeights) / 3 : 0;
    jdouble* groundData = groundCount > 0 ? env->GetDoubleArrayElements(groundHeights, nullptr) : nullptr;

    ravex::PortalPos result = ravex::findBestPortalPos(
        playerX, playerY, playerZ,
        playerYaw,
        minDist, maxDist,
        avoidPortalRange,
        portalData, portalCount,
        groundData, groundCount
    );

    jdouble* elements = env->GetDoubleArrayElements(out, nullptr);
    elements[0] = result.x;
    elements[1] = result.y;
    elements[2] = result.z;
    elements[3] = result.score;
    env->ReleaseDoubleArrayElements(out, elements, 0);

    if (portalData) env->ReleaseDoubleArrayElements(existingPortals, portalData, JNI_ABORT);
    if (groundData) env->ReleaseDoubleArrayElements(groundHeights, groundData, JNI_ABORT);
}
