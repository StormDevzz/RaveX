#include <jni.h>
#include "../../../common/include/jni_raii.hpp"
#include "discord_rpc.hpp"
#include "checks.hpp"

extern "C" {

JNIEXPORT void JNICALL
Java_ravex_modules_client_RichPresence_nativeInitialize(JNIEnv* env, jclass, jstring jClientId) {
    if (!jClientId) return;
    JniStringUtfChars clientIdGuard(env, jClientId);
    if (!clientIdGuard) return;
    if (ravex::jni::checkAndClearException(env)) return;
    DiscordRPC::initialize(clientIdGuard.get());
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_RichPresence_nativeShutdown(JNIEnv* env, jclass) {
    if (ravex::jni::checkAndClearException(env)) return;
    DiscordRPC::shutdown();
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_RichPresence_nativeUpdatePresence(
    JNIEnv* env, jclass,
    jstring jState, jstring jDetails, jlong startTimestamp, jboolean jShowOS,
    jboolean jShowButton, jstring jLargeImageKey
) {
    if (ravex::jni::checkAndClearException(env)) return;
    DiscordRichPresence presence;

    std::string baseState = "";
    if (jState) {
        JniStringUtfChars stateGuard(env, jState);
        if (!stateGuard) return;
        if (ravex::jni::checkAndClearException(env)) return;
        baseState = stateGuard.get();
    }

    if (jShowOS == JNI_TRUE) {
        std::string osInfo = ravex::checks::getOSInfo();
        if (!baseState.empty()) {
            presence.state = baseState + "\nOS: " + osInfo;
        } else {
            presence.state = "OS: " + osInfo;
        }
    } else {
        presence.state = baseState;
    }

    if (jDetails) {
        JniStringUtfChars detailsGuard(env, jDetails);
        if (!detailsGuard) return;
        if (ravex::jni::checkAndClearException(env)) return;
        presence.details = detailsGuard.get();
    }

    presence.startTimestamp = static_cast<int64_t>(startTimestamp);

    std::string largeImgKey = "icon";
    if (jLargeImageKey) {
        JniStringUtfChars imgKeyGuard(env, jLargeImageKey);
        if (!imgKeyGuard) return;
        if (ravex::jni::checkAndClearException(env)) return;
        largeImgKey = imgKeyGuard.get();
    }
    presence.largeImageKey = largeImgKey;
    presence.largeImageText = "RaveX";

    if (jShowButton == JNI_TRUE) {
        DiscordButton btn;
        btn.label = "Download RaveX";
        btn.url = "https://github.com/StormDevzz/RaveX";
        presence.buttons.push_back(btn);
    }

    DiscordRPC::updatePresence(presence);
    if (ravex::jni::checkAndClearException(env)) return;
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_client_RichPresence_nativeIsConnected(JNIEnv* env, jclass) {
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return DiscordRPC::isConnected() ? JNI_TRUE : JNI_FALSE;
}

}
