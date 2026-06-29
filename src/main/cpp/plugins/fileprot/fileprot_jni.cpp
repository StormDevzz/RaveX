#include <jni.h>
#include "include/fileprot.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jboolean JNICALL
Java_ravex_fileprot_FileProtBridge_nativeInit(
    JNIEnv* env, jclass, jstring dbPath)
{
    const char* path = env->GetStringUTFChars(dbPath, nullptr);
    bool ok = ravex::fileprot::initialize(path ? path : "");
    env->ReleaseStringUTFChars(dbPath, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ravex_fileprot_FileProtBridge_nativeShutdown(
    JNIEnv* env, jclass)
{
    ravex::fileprot::shutdown();
}

JNIEXPORT jboolean JNICALL
Java_ravex_fileprot_FileProtBridge_nativeBackupFile(
    JNIEnv* env, jclass, jstring filePath, jstring backupDir)
{
    const char* fp = env->GetStringUTFChars(filePath, nullptr);
    const char* bd = env->GetStringUTFChars(backupDir, nullptr);
    bool ok = ravex::fileprot::backupFile(fp, bd);
    env->ReleaseStringUTFChars(filePath, fp);
    env->ReleaseStringUTFChars(backupDir, bd);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_ravex_fileprot_FileProtBridge_nativeRestoreFile(
    JNIEnv* env, jclass, jstring backupPath, jstring targetPath)
{
    const char* bp = env->GetStringUTFChars(backupPath, nullptr);
    const char* tp = env->GetStringUTFChars(targetPath, nullptr);
    bool ok = ravex::fileprot::restoreFile(bp, tp);
    env->ReleaseStringUTFChars(backupPath, bp);
    env->ReleaseStringUTFChars(targetPath, tp);
    return ok ? JNI_TRUE : JNI_FALSE;
}

}
