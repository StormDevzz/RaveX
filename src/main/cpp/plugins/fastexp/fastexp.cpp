#include "fastexp.hpp"
#include <chrono>

namespace ravex {

FastXp::FastXp() : running(false), jvm(nullptr), cls(nullptr), mid(nullptr) {}

FastXp::~FastXp() {
    JNIEnv* env = nullptr;
    if (jvm && cls) {
        jvm->AttachCurrentThread((void**)&env, nullptr);
        stop(env);
    }
}

void FastXp::start(JavaVM* vm, jclass clazz, jmethodID callback) {
    if (running.load()) return;
    jvm = vm;
    mid = callback;
    JNIEnv* env = nullptr;
    vm->AttachCurrentThread((void**)&env, nullptr);
    cls = (jclass)env->NewGlobalRef(clazz);
    vm->DetachCurrentThread();
    running.store(true);
    worker = std::thread(&FastXp::run, this);
}

void FastXp::stop(JNIEnv* env) {
    running.store(false);
    if (worker.joinable()) worker.join();
    if (cls) {
        env->DeleteGlobalRef(cls);
        cls = nullptr;
    }
    mid = nullptr;
}

bool FastXp::isRunning() const {
    return running.load();
}

void FastXp::run() {
    JNIEnv* env = nullptr;
    jvm->AttachCurrentThread((void**)&env, nullptr);
    if (!env) return;

    while (running.load()) {
        if (cls && mid) {
            env->CallStaticVoidMethod(cls, mid);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(30));
    }

    jvm->DetachCurrentThread();
}

} 
