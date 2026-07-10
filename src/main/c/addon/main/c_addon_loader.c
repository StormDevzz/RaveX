#include "c_addon_api.h"
#include <jni.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_ADDONS 64

static struct {
    void*       handle;
    char*       name;
    ravex_c_addon_meta meta;
    void        (*on_tick)(void);
    void        (*on_key)(int, int);
} g_addons[MAX_ADDONS];

static int g_addon_count = 0;
static JavaVM* g_vm = NULL;

static void c_log_info(const char* msg) {
    fprintf(stdout, "[C Addon] %s\n", msg);
    fflush(stdout);
}

static void c_log_warn(const char* msg) {
    fprintf(stderr, "[C Addon WARN] %s\n", msg);
    fflush(stderr);
}

static void c_log_error(const char* msg) {
    fprintf(stderr, "[C Addon ERROR] %s\n", msg);
    fflush(stderr);
}

static const char* c_get_mc_version(void) {
    return "1.21";
}

static bool c_is_key_down(int key_code) {
    (void)key_code;
    return false;
}

static ravex_c_addon_api g_api = {
    .log_info       = c_log_info,
    .log_warn       = c_log_warn,
    .log_error      = c_log_error,
    .get_mc_version = c_get_mc_version,
    .is_key_down    = c_is_key_down,
};

static int try_load_c_addon(const char* path) {
    if (g_addon_count >= MAX_ADDONS) return -1;

    void* handle = dlopen(path, RTLD_LAZY | RTLD_LOCAL);
    if (!handle) {
        fprintf(stderr, "[C Addon] dlopen failed: %s\n", dlerror());
        return -1;
    }

    ravex_c_addon_meta* meta = dlsym(handle, "ravex_c_addon_meta_info");
    if (!meta) {
        fprintf(stderr, "[C Addon] missing ravex_c_addon_meta_info in %s\n", path);
        dlclose(handle);
        return -1;
    }

    if (meta->api_version != RAVEX_C_ADDON_API_VERSION) {
        fprintf(stderr, "[C Addon] api_version mismatch in %s\n", path);
        dlclose(handle);
        return -1;
    }

    ravex_c_addon_init_fn init_fn = dlsym(handle, "ravex_c_addon_init");
    if (!init_fn) {
        fprintf(stderr, "[C Addon] missing ravex_c_addon_init in %s\n", path);
        dlclose(handle);
        return -1;
    }

    int ret = init_fn(&g_api);
    if (ret != 0) {
        fprintf(stderr, "[C Addon] ravex_c_addon_init returned %d in %s\n", ret, path);
        dlclose(handle);
        return -1;
    }

    int idx = g_addon_count;
    g_addons[idx].handle  = handle;
    g_addons[idx].name    = strdup(meta->name ? meta->name : "unknown");
    g_addons[idx].meta    = *meta;
    g_addons[idx].on_tick = dlsym(handle, "ravex_c_addon_on_tick");
    g_addons[idx].on_key  = dlsym(handle, "ravex_c_addon_on_key");

    g_addon_count++;
    printf("[C Addon] loaded: %s v%s\n",
           meta->name ? meta->name : "?",
           meta->version ? meta->version : "?");
    return 0;
}

static void unload_all_c_addons(void) {
    for (int i = 0; i < g_addon_count; i++) {
        ravex_c_addon_shutdown_fn shutdown_fn = dlsym(g_addons[i].handle, "ravex_c_addon_shutdown");
        if (shutdown_fn) shutdown_fn();
        dlclose(g_addons[i].handle);
        free(g_addons[i].name);
    }
    g_addon_count = 0;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)reserved;
    g_vm = vm;
    return JNI_VERSION_21;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    (void)vm;
    (void)reserved;
    unload_all_c_addons();
}

JNIEXPORT void JNICALL
Java_ravex_addon_core_CAddonManager_nativeInitCAddons(
    JNIEnv* env, jclass cls, jobjectArray path_array) {

    (void)cls;
    if (!path_array) return;

    jsize count = (*env)->GetArrayLength(env, path_array);
    for (jsize i = 0; i < count; i++) {
        jstring path_j = (*env)->GetObjectArrayElement(env, path_array, i);
        if (!path_j) continue;

        const char* path = (*env)->GetStringUTFChars(env, path_j, NULL);
        if (path) {
            try_load_c_addon(path);
            (*env)->ReleaseStringUTFChars(env, path_j, path);
        }
        (*env)->DeleteLocalRef(env, path_j);
    }
}

JNIEXPORT void JNICALL
Java_ravex_addon_core_CAddonManager_nativeUnloadCAddons(
    JNIEnv* env, jclass cls) {

    (void)env; (void)cls;
    unload_all_c_addons();
}

JNIEXPORT void JNICALL
Java_ravex_addon_core_CAddonManager_nativeTickCAddons(
    JNIEnv* env, jclass cls) {

    (void)env; (void)cls;
    for (int i = 0; i < g_addon_count; i++) {
        if (g_addons[i].on_tick) g_addons[i].on_tick();
    }
}

JNIEXPORT void JNICALL
Java_ravex_addon_core_CAddonManager_nativeKeyEventCAddons(
    JNIEnv* env, jclass cls, jint key, jint action) {

    (void)env; (void)cls;
    for (int i = 0; i < g_addon_count; i++) {
        if (g_addons[i].on_key) g_addons[i].on_key((int)key, (int)action);
    }
}
