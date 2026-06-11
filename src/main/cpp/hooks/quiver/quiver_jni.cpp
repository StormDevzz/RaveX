#include <jni.h>
#include "quiver.h"
#include <vector>
#include <string>

using namespace ravex;

extern "C" {

JNIEXPORT jint JNICALL
Java_ravex_modules_combat_Quiver_nativeSelectBestArrow(
    JNIEnv* env, jclass cls,
    jobjectArray activeEffects,
    jintArray activeAmplifiers,
    jdoubleArray activeDurations,
    jobjectArray arrowEffects,
    jintArray arrowAmplifiers,
    jstring preferredType
) {
    std::vector<ActiveEffect> active;
    if (activeEffects && activeAmplifiers && activeDurations) {
        jsize active_len = env->GetArrayLength(activeEffects);
        jint* amps = env->GetIntArrayElements(activeAmplifiers, nullptr);
        jdouble* durs = env->GetDoubleArrayElements(activeDurations, nullptr);

        for (jsize i = 0; i < active_len; i++) {
            jstring jstr = (jstring)env->GetObjectArrayElement(activeEffects, i);
            if (jstr) {
                const char* chars = env->GetStringUTFChars(jstr, nullptr);
                active.push_back({std::string(chars), static_cast<int>(amps[i]), durs[i]});
                env->ReleaseStringUTFChars(jstr, chars);
                env->DeleteLocalRef(jstr);
            }
        }

        env->ReleaseIntArrayElements(activeAmplifiers, amps, JNI_ABORT);
        env->ReleaseDoubleArrayElements(activeDurations, durs, JNI_ABORT);
    }

    std::vector<ArrowData> arrows;
    if (arrowEffects && arrowAmplifiers) {
        jsize arrow_len = env->GetArrayLength(arrowEffects);
        jint* amps = env->GetIntArrayElements(arrowAmplifiers, nullptr);

        for (jsize i = 0; i < arrow_len; i++) {
            jstring jstr = (jstring)env->GetObjectArrayElement(arrowEffects, i);
            if (jstr) {
                const char* chars = env->GetStringUTFChars(jstr, nullptr);
                arrows.push_back({std::string(chars), static_cast<int>(amps[i])});
                env->ReleaseStringUTFChars(jstr, chars);
                env->DeleteLocalRef(jstr);
            } else {
                arrows.push_back({"", 0});
            }
        }

        env->ReleaseIntArrayElements(arrowAmplifiers, amps, JNI_ABORT);
    }

    std::string pref = "";
    if (preferredType) {
        const char* chars = env->GetStringUTFChars(preferredType, nullptr);
        pref = std::string(chars);
        env->ReleaseStringUTFChars(preferredType, chars);
    }

    return selectBestArrow(active, arrows, pref);
}

}
