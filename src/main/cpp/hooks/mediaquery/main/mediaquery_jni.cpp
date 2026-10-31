#include "mediaquery.hpp"
#include <jni.h>
#include <cstring>
#include "../../../common/include/jni_raii.hpp"

#ifdef __linux__
#include <dbus/dbus.h>
#endif

extern "C" {

JNIEXPORT jstring JNICALL
Java_ravex_modules_hud_NowPlayingHud_nativeGetNowPlaying(JNIEnv* env, jclass) {
    auto info = ravex::queryNowPlaying();
    if (!info.valid) {
        if (ravex::jni::checkAndClearException(env)) return nullptr;
        return env->NewStringUTF("");
    }
    std::string result = info.status + "|" + info.title + "|" + info.artist + "|" + info.artUrl;
    jstring out = env->NewStringUTF(result.c_str());
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return out;
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_NowPlayingHud_nativeDownloadArt(JNIEnv* env, jclass, jstring url) {
    if (!url) return nullptr;
    JniStringUtfChars urlGuard(env, url);
    if (!urlGuard) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    auto data = ravex::downloadArt(std::string(urlGuard.get()));
    if (data.empty()) return nullptr;
    jbyteArray result = env->NewByteArray(static_cast<jsize>(data.size()));
    if (!result) return nullptr;
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(data.size()), reinterpret_cast<const jbyte*>(data.data()));
    if (ravex::jni::checkAndClearException(env)) return nullptr;
    return result;
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_hud_NowPlayingHud_nativeIsAvailable(JNIEnv*, jclass) {
#ifdef _WIN32
    return JNI_TRUE;
#else
    DBusError err;
    dbus_error_init(&err);
    DBusConnection* conn = dbus_bus_get(DBUS_BUS_SESSION, &err);
    if (conn) {
        dbus_connection_unref(conn);
        dbus_error_free(&err);
        return JNI_TRUE;
    }
    dbus_error_free(&err);
    return JNI_FALSE;
#endif
}

JNIEXPORT jstring JNICALL
Java_ravex_modules_hud_MediaHud_nativeGetNowPlaying(JNIEnv* env, jclass cls) {
    return Java_ravex_modules_hud_NowPlayingHud_nativeGetNowPlaying(env, cls);
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_MediaHud_nativeDownloadArt(JNIEnv* env, jclass cls, jstring url) {
    return Java_ravex_modules_hud_NowPlayingHud_nativeDownloadArt(env, cls, url);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_hud_MediaHud_nativeIsAvailable(JNIEnv* env, jclass cls) {
    return Java_ravex_modules_hud_NowPlayingHud_nativeIsAvailable(env, cls);
}

}
