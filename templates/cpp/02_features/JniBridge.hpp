// ══════════════════════════════════════════════════════════════════════════════
//  02_features / JniBridge.hpp
//
//  RU: JNI-мост для двусторонней связи между Java и C++.
//      Этот заголовок предоставляет удобные утилиты, чтобы:
//        - получить JNIEnv из любого потока (getEnv)
//        - вызвать Java-метод из C++ (fireCallback)
//        - конвертировать jstring в std::string и обратно
//
//      Использование из Java:
//        public class MyAddon implements Addon {
//            public static native void nativeInit(String msg);
//            public static native int nativeAdd(int a, int b);
//            public static native void nativeSetKeybind(int key);
//            public static native String nativeGetStatus();
//        }
//        // Загрузка: System.loadLibrary("MyAddon");
//
//  EN: JNI bridge for bidirectional communication between Java and C++.
//      This header provides convenient utilities to:
//        - get JNIEnv from any thread (getEnv)
//        - call a Java method from C++ (fireCallback)
//        - convert jstring to std::string and back
//
//      Usage from Java:
//        public class MyAddon implements Addon {
//            public static native void nativeInit(String msg);
//            public static native int nativeAdd(int a, int b);
//            public static native void nativeSetKeybind(int key);
//            public static native String nativeGetStatus();
//        }
//        // Loading: System.loadLibrary("MyAddon");
// ══════════════════════════════════════════════════════════════════════════════

#pragma once
#include <jni.h>
#include <string>

// RU: Все функции и утилиты находятся в пространстве имён ravex::addon::jni.
// EN: All functions and utilities are in the ravex::addon::jni namespace.
namespace ravex {
namespace addon {
namespace jni {

// RU: Вызывается один раз при загрузке JNI-библиотеки (из JNI_OnLoad).
//     Сохраняет указатель на JavaVM для последующего получения JNIEnv.
// EN: Called once when the JNI library is loaded (from JNI_OnLoad).
//     Saves the JavaVM pointer for later JNIEnv retrieval.
void onLoad(JavaVM* vm);

// RU: Получает JNIEnv для текущего потока.
//     Если текущий поток ещё не прикреплён к JVM — автоматически
//     прикрепляет его. Это нужно для вызова Java-методов
//     из C++ потоков, которые не были созданы Java.
// EN: Gets JNIEnv for the current thread.
//     If the current thread is not attached to the JVM yet —
//     automatically attaches it. This is needed for calling Java
//     methods from C++ threads that were not created by Java.
JNIEnv* getEnv();

// RU: Вызывает статический void-метод Java-класса из C++.
//     Пример:
//       fireCallback("onData", "(Ljava/lang/String;)V", arg);
//     это вызовет Java_класс.onData(String) из C++.
//     Используется для обратных вызовов (callbacks).
// EN: Calls a static void method on a Java class from C++.
//     Example:
//       fireCallback("onData", "(Ljava/lang/String;)V", arg);
//     this calls Java_class.onData(String) from C++.
//     Used for callbacks.
void fireCallback(const char* method, const char* sig, jvalue arg);

// RU: Конвертирует jstring в std::string.
//     Автоматически вызывает GetStringUTFChars и ReleaseStringUTFChars,
//     чтобы не забыть освободить память.
// EN: Converts jstring to std::string.
//     Automatically calls GetStringUTFChars and ReleaseStringUTFChars,
//     so you don't forget to free the memory.
inline std::string jstring2str(JNIEnv* env, jstring js) {
    if (!js || !env) return {};
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string out(chars);
    env->ReleaseStringUTFChars(js, chars);
    return out;
}

// RU: Конвертирует std::string в jstring.
//     Возвращает nullptr, если env равен nullptr.
// EN: Converts std::string to jstring.
//     Returns nullptr if env is nullptr.
inline jstring str2jstring(JNIEnv* env, const std::string& s) {
    return env ? env->NewStringUTF(s.c_str()) : nullptr;
}

} // namespace jni
} // namespace addon
} // namespace ravex
