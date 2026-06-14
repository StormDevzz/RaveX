#include "coordlogger_jni.h"
#include "coordlogger.h"

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_CoordLogger_nativeEnsureDir(JNIEnv* env, jclass cls, jstring path) {
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    bool result = ensureDir(cpath);
    env->ReleaseStringUTFChars(path, cpath);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_misc_CoordLogger_nativeWriteLog(JNIEnv* env, jclass cls, jstring filePath, jstring content) {
    const char* cpath = env->GetStringUTFChars(filePath, nullptr);
    const char* ccontent = env->GetStringUTFChars(content, nullptr);
    bool result = writeLog(cpath, ccontent);
    env->ReleaseStringUTFChars(filePath, cpath);
    env->ReleaseStringUTFChars(content, ccontent);
    return result ? JNI_TRUE : JNI_FALSE;
}
