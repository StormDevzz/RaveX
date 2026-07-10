#include <jni.h>
#include "discord_rpc.hpp"
#include "checks.hpp"

extern "C" {

JNIEXPORT void JNICALL
Java_ravex_modules_player_RichPresence_nativeInitialize(JNIEnv* env, jclass, jstring jClientId) {
    if (!jClientId) return;
    const char* clientId = env->GetStringUTFChars(jClientId, nullptr);
    DiscordRPC::initialize(clientId);
    env->ReleaseStringUTFChars(jClientId, clientId);
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_RichPresence_nativeShutdown(JNIEnv* env, jclass) {
    DiscordRPC::shutdown();
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_RichPresence_nativeUpdatePresence(
    JNIEnv* env, jclass,
    jstring jState, jstring jDetails, jlong startTimestamp, jboolean jShowOS,
    jboolean jShowButton, jstring jLargeImageKey
) {
    DiscordRichPresence presence;

    std::string baseState = "";
    if (jState) {
        const char* state = env->GetStringUTFChars(jState, nullptr);
        baseState = state;
        env->ReleaseStringUTFChars(jState, state);
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
        const char* details = env->GetStringUTFChars(jDetails, nullptr);
        presence.details = details;
        env->ReleaseStringUTFChars(jDetails, details);
    }

    presence.startTimestamp = static_cast<int64_t>(startTimestamp);

    std::string largeImgKey = "icon";
    if (jLargeImageKey) {
        const char* imgKey = env->GetStringUTFChars(jLargeImageKey, nullptr);
        largeImgKey = imgKey;
        env->ReleaseStringUTFChars(jLargeImageKey, imgKey);
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
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_player_RichPresence_nativeIsConnected(JNIEnv* env, jclass) {
    return DiscordRPC::isConnected() ? JNI_TRUE : JNI_FALSE;
}

}
