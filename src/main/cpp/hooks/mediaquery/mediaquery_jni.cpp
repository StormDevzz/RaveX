#include "mediaquery.hpp"
#include <jni.h>
#include <cstring>

#ifdef __linux__
#include <dbus/dbus.h>
#endif

extern "C" {

JNIEXPORT jstring JNICALL
Java_ravex_modules_hud_NowPlayingHud_nativeGetNowPlaying(JNIEnv* env, jclass) {
    auto info = ravex::queryNowPlaying();
    if (!info.valid) {
        return env->NewStringUTF("");
    }
    std::string result = info.status + "|" + info.title + "|" + info.artist + "|" + info.artUrl;
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_NowPlayingHud_nativeDownloadArt(JNIEnv* env, jclass, jstring url) {
    if (!url) return nullptr;
    const char* urlStr = env->GetStringUTFChars(url, nullptr);
    auto data = ravex::downloadArt(std::string(urlStr));
    env->ReleaseStringUTFChars(url, urlStr);
    if (data.empty()) return nullptr;
    jbyteArray result = env->NewByteArray(static_cast<jsize>(data.size()));
    if (!result) return nullptr;
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(data.size()), reinterpret_cast<const jbyte*>(data.data()));
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

}
