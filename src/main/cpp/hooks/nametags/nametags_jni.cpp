#include "nametags_jni.h"
#include "nametags.h"

JNIEXPORT jdoubleArray JNICALL
Java_ravex_modules_render_NameTags_nativeCalculateLayout(
    JNIEnv* env, jclass cls,
    jdouble distance,
    jdouble scaleParam,
    jboolean distanceScaling,
    jboolean showArmor,
    jboolean showHands,
    jboolean hasOwner,
    jdouble tw,
    jdouble ow,
    jboolean hasMainHand,
    jboolean hasOffHand,
    jint armorCount
) {
    NameTagsLayout layout = calculateNameTagsLayout(
        distance,
        scaleParam,
        distanceScaling == JNI_TRUE,
        showArmor == JNI_TRUE,
        showHands == JNI_TRUE,
        hasOwner == JNI_TRUE,
        tw,
        ow,
        hasMainHand == JNI_TRUE,
        hasOffHand == JNI_TRUE,
        armorCount
    );

    jdoubleArray out = env->NewDoubleArray(10);
    if (!out) return nullptr;

    jdouble buf[10] = {
        layout.scale,
        layout.totalWidth,
        layout.totalHeight,
        layout.armorRowY,
        layout.mainRowY,
        layout.ownerRowY,
        layout.textYOffset,
        layout.mainRowWidth,
        layout.armorRowWidth,
        0.0 
    };

    env->SetDoubleArrayRegion(out, 0, 10, buf);
    return out;
}
