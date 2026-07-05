#include "proxy_jni.hpp"
#include "proxy_handler.hpp"
#include <logger.hpp>
#include <cstring>

using namespace packet;

static proxy::LocalProxy* g_instance = nullptr;

JNIEXPORT jint JNICALL
Java_ravex_proxy_ProxyNative_startLocalProxy(
    JNIEnv* env, jclass clazz,
    jstring proxyType,
    jstring proxyHost, jint proxyPort,
    jstring targetHost, jint targetPort,
    jstring username, jstring password,
    jint listenPort)
{
    if (g_instance) {
        g_instance->stop();
        delete g_instance;
        g_instance = nullptr;
    }

    if (!proxyType || !proxyHost || !targetHost) return -1;

    const char* typeStr = env->GetStringUTFChars(proxyType, nullptr);
    const char* hostStr = env->GetStringUTFChars(proxyHost, nullptr);
    const char* targetStr = env->GetStringUTFChars(targetHost, nullptr);
    const char* userStr = username ? env->GetStringUTFChars(username, nullptr) : "";
    const char* passStr = password ? env->GetStringUTFChars(password, nullptr) : "";

    proxy::Config cfg;
    cfg.proxyHost = hostStr;
    cfg.proxyPort = static_cast<uint16_t>(proxyPort);
    cfg.targetHost = targetStr;
    cfg.targetPort = static_cast<uint16_t>(targetPort);
    cfg.useAuth = (strlen(userStr) > 0);
    cfg.username = userStr;
    cfg.password = passStr;

    if (strcmp(typeStr, "SOCKS5") == 0) cfg.type = proxy::Type::SOCKS5;
    else if (strcmp(typeStr, "SOCKS4") == 0) cfg.type = proxy::Type::SOCKS4;
    else if (strcmp(typeStr, "HTTP") == 0) cfg.type = proxy::Type::HTTP;
    else cfg.type = proxy::Type::SOCKS5;

    env->ReleaseStringUTFChars(proxyType, typeStr);
    env->ReleaseStringUTFChars(proxyHost, hostStr);
    env->ReleaseStringUTFChars(targetHost, targetStr);
    if (username && userStr) env->ReleaseStringUTFChars(username, userStr);
    if (password && passStr) env->ReleaseStringUTFChars(password, passStr);

    g_instance = new proxy::LocalProxy();
    if (!g_instance->start(cfg, static_cast<uint16_t>(listenPort))) {
        delete g_instance;
        g_instance = nullptr;
        return -1;
    }

    return static_cast<jint>(g_instance->getPort());
}

JNIEXPORT void JNICALL
Java_ravex_proxy_ProxyNative_stopLocalProxy(
    JNIEnv* env, jclass clazz)
{
    if (g_instance) {
        g_instance->stop();
        delete g_instance;
        g_instance = nullptr;
        log::info("[proxy] local proxy stopped");
    }
}
