#ifndef RAVEX_FASTXP_H
#define RAVEX_FASTXP_H

#include <atomic>
#include <thread>
#include <jni.h>

namespace ravex {

class FastXp {
public:
    FastXp();
    ~FastXp();

    void start(JavaVM* jvm, jclass clazz, jmethodID callback);
    void stop(JNIEnv* env);
    bool isRunning() const;

private:
    void run();

    std::atomic<bool> running;
    std::thread worker;
    JavaVM* jvm;
    jclass cls;
    jmethodID mid;
};

} // namespace ravex

#endif
