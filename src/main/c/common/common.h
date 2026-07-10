#pragma once

#include <jni.h>
#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

#define RAVEX_MODULE_VERSION "1.0.0"

typedef struct {
    const char* name;
    const char* description;
    const char* version;
} ravex_module_info;

const char* ravex_strdup(JNIEnv* env, jstring str);
jstring ravex_to_java_string(JNIEnv* env, const char* str);
void ravex_throw(JNIEnv* env, const char* msg);
int32_t* ravex_jintarray_to_c(JNIEnv* env, jintArray arr, jsize* out_len);
void ravex_release_jintarray(JNIEnv* env, jintArray arr, int32_t* data);
float* ravex_jfloatarray_to_c(JNIEnv* env, jfloatArray arr, jsize* out_len);
void ravex_release_jfloatarray(JNIEnv* env, jfloatArray arr, float* data);
