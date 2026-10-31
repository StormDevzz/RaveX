#include <jni.h>
#include "../../../common/include/jni_raii.hpp"
#include "quiver.hpp"
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
        if (ravex::jni::checkAndClearException(env)) return 0;

        JniIntElements ampsGuard(env, activeAmplifiers);
        JniDoubleElements dursGuard(env, activeDurations);
        if (ravex::jni::checkAndClearException(env)) return 0;
        if (!ampsGuard || !dursGuard) return 0;

        const jint* amps = ampsGuard.get();
        const jdouble* durs = dursGuard.get();

        for (jsize i = 0; i < active_len; i++) {
            jstring jstr = (jstring)env->GetObjectArrayElement(activeEffects, i);
            if (ravex::jni::checkAndClearException(env)) return 0;
            if (jstr) {
                JniStringUtfChars charsGuard(env, jstr);
                if (!charsGuard) {
                    env->DeleteLocalRef(jstr);
                    continue;
                }
                if (ravex::jni::checkAndClearException(env)) return 0;
                active.push_back({std::string(charsGuard.get()), static_cast<int>(amps[i]), durs[i]});
                env->DeleteLocalRef(jstr);
            }
        }
    }

    std::vector<ArrowData> arrows;
    if (arrowEffects && arrowAmplifiers) {
        jsize arrow_len = env->GetArrayLength(arrowEffects);
        if (ravex::jni::checkAndClearException(env)) return 0;

        JniIntElements ampsGuard(env, arrowAmplifiers);
        if (ravex::jni::checkAndClearException(env)) return 0;
        if (!ampsGuard) return 0;

        const jint* amps = ampsGuard.get();

        for (jsize i = 0; i < arrow_len; i++) {
            jstring jstr = (jstring)env->GetObjectArrayElement(arrowEffects, i);
            if (ravex::jni::checkAndClearException(env)) return 0;
            if (jstr) {
                JniStringUtfChars charsGuard(env, jstr);
                if (!charsGuard) {
                    env->DeleteLocalRef(jstr);
                    continue;
                }
                if (ravex::jni::checkAndClearException(env)) return 0;
                arrows.push_back({std::string(charsGuard.get()), static_cast<int>(amps[i])});
                env->DeleteLocalRef(jstr);
            } else {
                arrows.push_back({"", 0});
            }
        }
    }

    std::string pref = "";
    if (preferredType) {
        JniStringUtfChars charsGuard(env, preferredType);
        if (!charsGuard) return 0;
        if (ravex::jni::checkAndClearException(env)) return 0;
        pref = std::string(charsGuard.get());
    }

    return selectBestArrow(active, arrows, pref);
}

}
