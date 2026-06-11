#include <jni.h>
#include "desktopgui.h"
#include <string>
#include <vector>

static JavaVM* cached_jvm = nullptr;
static jclass desktopgui_class = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    cached_jvm = vm;
    return JNI_VERSION_1_8;
}

static JNIEnv* get_env() {
    JNIEnv* env = nullptr;
    if (cached_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
        cached_jvm->AttachCurrentThread((void**)&env, nullptr);
    }
    return env;
}

void notify_java_toggle(const std::string& name) {
    JNIEnv* env = get_env();
    if (!env || !desktopgui_class) return;
    
    jmethodID method = env->GetStaticMethodID(desktopgui_class, "toggleModuleFromNative", "(Ljava/lang/String;)V");
    if (method) {
        jstring jname = env->NewStringUTF(name.c_str());
        env->CallStaticVoidMethod(desktopgui_class, method, jname);
        env->DeleteLocalRef(jname);
    }
}

void notify_java_close() {
    JNIEnv* env = get_env();
    if (!env || !desktopgui_class) return;
    
    jmethodID method = env->GetStaticMethodID(desktopgui_class, "onNativeClose", "()V");
    if (method) {
        env->CallStaticVoidMethod(desktopgui_class, method);
    }
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_openDesktopGui(JNIEnv* env, jclass cls, jobjectArray names, jbooleanArray states) {
    if (desktopgui_class) {
        env->DeleteGlobalRef(desktopgui_class);
    }
    desktopgui_class = (jclass)env->NewGlobalRef(cls);

    std::vector<ravex::ModuleGuiData> modules;
    jsize len = env->GetArrayLength(names);
    jboolean* states_buf = env->GetBooleanArrayElements(states, nullptr);

    for (jsize i = 0; i < len; i++) {
        jstring jname = (jstring)env->GetObjectArrayElement(names, i);
        const char* name_chars = env->GetStringUTFChars(jname, nullptr);
        
        modules.push_back({std::string(name_chars), states_buf[i] == JNI_TRUE});
        
        env->ReleaseStringUTFChars(jname, name_chars);
        env->DeleteLocalRef(jname);
    }

    env->ReleaseBooleanArrayElements(states, states_buf, JNI_ABORT);

    ravex::start_gui(modules);
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_updateModuleState(JNIEnv* env, jclass cls, jstring name, jboolean enabled) {
    const char* name_chars = env->GetStringUTFChars(name, nullptr);
    ravex::update_gui_state(std::string(name_chars), enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(name, name_chars);
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_closeDesktopGui(JNIEnv* env, jclass cls) {
    ravex::stop_gui();
}

}
