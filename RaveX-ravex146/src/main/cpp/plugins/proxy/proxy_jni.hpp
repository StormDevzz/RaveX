#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_ravex_utility_network_ProxyNative_startLocalProxy(
    JNIEnv* env, jclass clazz,
    jstring proxyType,
    jstring proxyHost, jint proxyPort,
    jstring targetHost, jint targetPort,
    jstring username, jstring password,
    jint listenPort
);

JNIEXPORT void JNICALL
Java_ravex_utility_network_ProxyNative_stopLocalProxy(
    JNIEnv* env, jclass clazz
);

#ifdef __cplusplus
}
#endif
