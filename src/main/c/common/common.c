#include "common.h"
#include <stdlib.h>
#include <string.h>

const char* ravex_strdup(JNIEnv* env, jstring str) {
    if (!str) return NULL;
    const char* utf = (*env)->GetStringUTFChars(env, str, NULL);
    if (!utf) return NULL;
    char* copy = strdup(utf);
    (*env)->ReleaseStringUTFChars(env, str, utf);
    return copy;
}

jstring ravex_to_java_string(JNIEnv* env, const char* str) {
    if (!str) return NULL;
    return (*env)->NewStringUTF(env, str);
}

void ravex_throw(JNIEnv* env, const char* msg) {
    jclass ex = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (ex)
        (*env)->ThrowNew(env, ex, msg);
}

int32_t* ravex_jintarray_to_c(JNIEnv* env, jintArray arr, jsize* out_len) {
    if (!arr) { *out_len = 0; return NULL; }
    *out_len = (*env)->GetArrayLength(env, arr);
    int32_t* data = malloc(*out_len * sizeof(int32_t));
    if (!data) return NULL;
    (*env)->GetIntArrayRegion(env, arr, 0, *out_len, data);
    return data;
}

void ravex_release_jintarray(JNIEnv* env, jintArray arr, int32_t* data) {
    (void)env; (void)arr;
    free(data);
}

float* ravex_jfloatarray_to_c(JNIEnv* env, jfloatArray arr, jsize* out_len) {
    if (!arr) { *out_len = 0; return NULL; }
    *out_len = (*env)->GetArrayLength(env, arr);
    float* data = malloc(*out_len * sizeof(float));
    if (!data) return NULL;
    (*env)->GetFloatArrayRegion(env, arr, 0, *out_len, data);
    return data;
}

void ravex_release_jfloatarray(JNIEnv* env, jfloatArray arr, float* data) {
    (void)env; (void)arr;
    free(data);
}
