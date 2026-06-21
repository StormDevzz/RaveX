// ══════════════════════════════════════════════════════════════════════════════
//  02_features / JniBridge.cpp
//
//  RU: Реализация JNI-моста. Содержит:
//      - onLoad() — сохранение JavaVM
//      - getEnv() — получение JNIEnv (с автоматическим AttachCurrentThread)
//      - fireCallback() — вызов Java-метода из C++
//      - JNI_OnLoad() — стандартная точка входа JNI
//      - Примеры native-функций: nativeInit, nativeAdd, nativeGetPlatformInfo...
//
//      Эти функции можно использовать как из Java (через native-объявления),
//      так и из C++ (через fireCallback).
//
//  EN: JNI bridge implementation. Contains:
//      - onLoad() — save JavaVM
//      - getEnv() — get JNIEnv (with automatic AttachCurrentThread)
//      - fireCallback() — call Java method from C++
//      - JNI_OnLoad() — standard JNI entry point
//      - Example native functions: nativeInit, nativeAdd, nativeGetPlatformInfo...
//
//      These functions can be used both from Java (via native declarations)
//      and from C++ (via fireCallback).
// ══════════════════════════════════════════════════════════════════════════════

#include "JniBridge.hpp"
#include "platform.hpp"
#include <cstdio>
#include <cstring>
#include <map>
#include <mutex>

namespace ravex {
namespace addon {
namespace jni {

// RU: Статические глобальные переменные:
//     g_vm — указатель на JavaVM (устанавливается в onLoad)
//     g_cls — глобальная ссылка на Java-класс (устанавливается из nativeInit)
//     g_obj — глобальная ссылка на Java-объект (если нужны нестатические методы)
//     g_mtx — мьютекс для потокобезопасности
// EN: Static global variables:
//     g_vm — JavaVM pointer (set in onLoad)
//     g_cls — global reference to the Java class (set from nativeInit)
//     g_obj — global reference to the Java object (for non-static methods)
//     g_mtx — mutex for thread safety
static JavaVM*  g_vm     = nullptr;
static jclass   g_cls    = nullptr;
static jobject  g_obj    = nullptr;
static std::mutex g_mtx;

// RU: Сохраняет указатель на JavaVM. Должна быть вызвана из JNI_OnLoad.
//     Без этого нельзя получить JNIEnv в произвольном потоке.
// EN: Saves the JavaVM pointer. Must be called from JNI_OnLoad.
//     Without this you cannot get JNIEnv in an arbitrary thread.
void onLoad(JavaVM* vm) {
    g_vm = vm;
}

// RU: Возвращает JNIEnv для текущего потока.
//     Если поток не прикреплён к JVM — автоматически прикрепляет.
//     Это необходимо, потому что C++ потоки (std::thread) не являются
//     Java-потоками и не имеют JNIEnv по умолчанию.
// EN: Returns JNIEnv for the current thread.
//     If the thread is not attached to JVM — automatically attaches it.
//     This is necessary because C++ threads (std::thread) are not
//     Java threads and don't have JNIEnv by default.
JNIEnv* getEnv() {
    if (!g_vm) return nullptr;
    JNIEnv* env = nullptr;
    if (g_vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
        // RU: Прикрепляем поток. После этого Java может вызывать методы
        //     этого потока, а поток может использовать JNI.
        // EN: Attach the thread. After this Java can call methods on
        //     this thread, and the thread can use JNI.
        g_vm->AttachCurrentThread((void**)&env, nullptr);
    }
    return env;
}

// RU: Вызывает статический Void-метод Java-класса из C++.
//     method — имя метода (например "onData")
//     sig — JNI-сигнатура (например "(Ljava/lang/String;)V")
//     arg — аргумент (jvalue)
//     Пример: fireCallback("onData", "(Ljava/lang/String;)V", arg)
//     вызовет Class.onData(String) из C++.
// EN: Calls a static void method on a Java class from C++.
//     method — method name (e.g. "onData")
//     sig — JNI signature (e.g. "(Ljava/lang/String;)V")
//     arg — argument (jvalue)
//     Example: fireCallback("onData", "(Ljava/lang/String;)V", arg)
//     calls Class.onData(String) from C++.
void fireCallback(const char* method, const char* sig, jvalue arg) {
    std::lock_guard<std::mutex> lock(g_mtx);
    JNIEnv* env = getEnv();
    if (!env || !g_cls) return;
    jmethodID mid = env->GetStaticMethodID(g_cls, method, sig);
    if (mid) {
        env->CallStaticVoidMethodA(g_cls, mid, &arg);
    }
}

// ─── JNI реализации / JNI implementations ────────────────────────────────────

extern "C" {

// RU: JNI_OnLoad — точка входа, вызываемая Java при System.loadLibrary().
//     Сохраняем vm и возвращаем минимальную версию JNI.
// EN: JNI_OnLoad — entry point called by Java on System.loadLibrary().
//     Save vm and return minimum JNI version.
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    onLoad(vm);
    return JNI_VERSION_1_8;
}

// RU: nativeInit — вызывается из Java для инициализации JNI-моста.
//     Сохраняет глобальную ссылку на класс (чтобы можно было вызывать
//     его методы позже из fireCallback).
//     jstring msg — приветственное сообщение из Java.
// EN: nativeInit — called from Java to initialize the JNI bridge.
//     Saves a global reference to the class (so its methods can be
//     called later via fireCallback).
//     jstring msg — welcome message from Java.
JNIEXPORT void JNICALL
Java_ravex_addon_jni_JniBridge_nativeInit(JNIEnv* env, jclass cls, jstring msg) {
    // RU: NewGlobalRef — создаём глобальную ссылку, чтобы класс
    //     не был выгружен сборщиком мусора.
    // EN: NewGlobalRef — create a global reference so the class
    //     is not garbage collected.
    g_cls = (jclass)env->NewGlobalRef(cls);
    std::string str = jstring2str(env, msg);
    std::printf("[JniBridge] Initialized: %s\n", str.c_str());
}

// RU: Пример простой native-функции: складывает два числа.
//     Демонстрирует передачу int из Java в C++ и обратно.
// EN: Example simple native function: adds two numbers.
//     Demonstrates passing int from Java to C++ and back.
JNIEXPORT jint JNICALL
Java_ravex_addon_jni_JniBridge_nativeAdd(JNIEnv*, jclass, jint a, jint b) {
    return a + b;
}

// RU: Возвращает строку с информацией о платформе.
//     Демонстрирует, как вернуть строку из C++ в Java.
//     Использует ADDON_GET_TOTAL_RAM_MB() из platform.hpp.
// EN: Returns a string with platform information.
//     Demonstrates returning a string from C++ to Java.
//     Uses ADDON_GET_TOTAL_RAM_MB() from platform.hpp.
JNIEXPORT jstring JNICALL
Java_ravex_addon_jni_JniBridge_nativeGetPlatformInfo(JNIEnv* env, jclass) {
    std::string info;
#ifdef _WIN32
    info = "Windows " + std::to_string(ADDON_GET_TOTAL_RAM_MB()) + "MB RAM";
#else
    info = "Linux " + std::to_string(ADDON_GET_TOTAL_RAM_MB()) + "MB RAM";
#endif
    return str2jstring(env, info);
}

// RU: Устанавливает высокий или нормальный приоритет процесса.
//     Демонстрирует передачу boolean из Java в C++ и использование
//     ADDON_SET_HIGH_PRIORITY / ADDON_SET_NORMAL_PRIORITY.
// EN: Sets high or normal process priority.
//     Demonstrates passing boolean from Java to C++ and using
//     ADDON_SET_HIGH_PRIORITY / ADDON_SET_NORMAL_PRIORITY.
JNIEXPORT void JNICALL
Java_ravex_addon_jni_JniBridge_nativeSetPriority(JNIEnv*, jclass, jboolean high) {
    if (high) {
        ADDON_SET_HIGH_PRIORITY();
        std::puts("[JniBridge] High priority set");
    } else {
        ADDON_SET_NORMAL_PRIORITY();
        std::puts("[JniBridge] Normal priority set");
    }
}

// RU: Очищает память процесса (working set).
//     На Windows — EmptyWorkingSet, на Linux — malloc_trim(0).
//     Полезно после интенсивной работы с памятью.
// EN: Trims the process working set.
//     On Windows — EmptyWorkingSet, on Linux — malloc_trim(0).
//     Useful after intensive memory work.
JNIEXPORT void JNICALL
Java_ravex_addon_jni_JniBridge_nativeTrimMemory(JNIEnv*, jclass) {
    ADDON_TRIM_MEMORY();
    std::puts("[JniBridge] Memory trimmed");
}

} // extern "C"
