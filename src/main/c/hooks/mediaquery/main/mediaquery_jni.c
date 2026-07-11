#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <inttypes.h>
#include "mediaquery.h"
#include "image_util.h"

JNIEXPORT jstring JNICALL
Java_ravex_modules_hud_MediaHud_nativeGetNowPlaying(JNIEnv* env, jclass cls) {
    (void)cls;
    MediaInfo* info = mediaquery_query();
    if (!info || !info->valid) {
        mediaquery_free(info);
        return (*env)->NewStringUTF(env, "");
    }
    char result[16384];
    int n = snprintf(result, sizeof(result), "%s|%s|%s|%s|%s|%" PRId64 "|%" PRId64,
        info->status ? info->status : "",
        info->title ? info->title : "",
        info->artist ? info->artist : "",
        info->artUrl ? info->artUrl : "",
        info->playerName ? info->playerName : "",
        info->position,
        info->length);
    mediaquery_free(info);
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_MediaHud_nativeDownloadArt(JNIEnv* env, jclass cls, jstring url) {
    (void)cls;
    if (!url) return NULL;
    const char* url_str = (*env)->GetStringUTFChars(env, url, NULL);
    if (!url_str) return NULL;
    size_t len;
    uint8_t* data = mediaquery_download_art(url_str, &len);
    (*env)->ReleaseStringUTFChars(env, url, url_str);
    if (!data || len == 0) { free(data); return NULL; }
    jbyteArray result = (*env)->NewByteArray(env, (jsize)len);
    if (!result) { free(data); return NULL; }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)len, (const jbyte*)data);
    free(data);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_hud_MediaHud_nativeIsAvailable(JNIEnv* env, jclass cls) {
    (void)env; (void)cls;
    MediaInfo* info = mediaquery_query();
    bool avail = (info != NULL && info->valid);
    mediaquery_free(info);
    return avail ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_MediaHud_nativeExtractAppIcon(JNIEnv* env, jclass cls) {
    (void)cls;
    MediaInfo* info = mediaquery_query();
    if (!info || !info->playerName) { mediaquery_free(info); return NULL; }
    size_t len;
    uint8_t* data = mediaquery_extract_icon(info->playerName, &len);
    if (!data || len == 0) { free(data); mediaquery_free(info); return NULL; }
    jbyteArray result = (*env)->NewByteArray(env, (jsize)len);
    if (!result) { free(data); mediaquery_free(info); return NULL; }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)len, (const jbyte*)data);
    free(data);
    mediaquery_free(info);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_MediaHud_nativeExtractAppIconForPlayer(JNIEnv* env, jclass cls, jstring playerName) {
    (void)cls;
    if (!playerName) return NULL;
    const char* name = (*env)->GetStringUTFChars(env, playerName, NULL);
    if (!name) return NULL;
    size_t len;
    uint8_t* data = mediaquery_extract_icon(name, &len);
    (*env)->ReleaseStringUTFChars(env, playerName, name);
    if (!data || len == 0) { free(data); return NULL; }
    jbyteArray result = (*env)->NewByteArray(env, (jsize)len);
    if (!result) { free(data); return NULL; }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)len, (const jbyte*)data);
    free(data);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_ravex_modules_hud_MediaHud_nativeGetCover(JNIEnv* env, jclass cls, jstring artUrl, jstring playerName, jint targetSize) {
    size_t rawLen = 0;
    uint8_t* rawData = NULL;

    if (artUrl) {
        const char* url = (*env)->GetStringUTFChars(env, artUrl, NULL);
        if (url) {
            rawData = mediaquery_download_art(url, &rawLen);
            (*env)->ReleaseStringUTFChars(env, artUrl, url);
        }
    }

    if (!rawData && playerName) {
        const char* name = (*env)->GetStringUTFChars(env, playerName, NULL);
        if (name) {
            rawData = mediaquery_extract_icon(name, &rawLen);
            (*env)->ReleaseStringUTFChars(env, playerName, name);
        }
    }

    if (!rawData || rawLen == 0) { free(rawData); return NULL; }

    int w = 0, h = 0;
    uint8_t* rgba = decode_to_rgba(rawData, rawLen, &w, &h);
    free(rawData);

    if (!rgba) return NULL;

    if (w != targetSize || h != targetSize) {
        uint8_t* resized = resize_rgba(rgba, w, h, targetSize, targetSize);
        free_image(rgba);
        rgba = resized;
    }

    if (!rgba) return NULL;

    size_t outLen = (size_t)targetSize * targetSize * 4;
    jbyteArray result = (*env)->NewByteArray(env, (jsize)outLen);
    if (!result) { free(rgba); return NULL; }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)outLen, (const jbyte*)rgba);
    free(rgba);
    return result;
}
