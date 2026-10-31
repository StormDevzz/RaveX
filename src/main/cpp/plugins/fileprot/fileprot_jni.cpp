#include <jni.h>
#include "../../../common/include/jni_raii.hpp"
#include "include/fileprot.hpp"

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    return JNI_VERSION_1_8;
}

JNIEXPORT jboolean JNICALL
Java_ravex_gui_fileprot_FileProtBridge_nativeInit(
    JNIEnv* env, jclass, jstring dbPath)
{
    JniStringUtfChars pathGuard(env, dbPath);
    if (!pathGuard) return JNI_FALSE;
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    bool ok = ravex::fileprot::initialize(pathGuard.get());
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ravex_gui_fileprot_FileProtBridge_nativeShutdown(
    JNIEnv* env, jclass)
{
    if (ravex::jni::checkAndClearException(env)) return;
    ravex::fileprot::shutdown();
}

JNIEXPORT jboolean JNICALL
Java_ravex_gui_fileprot_FileProtBridge_nativeBackupFile(
    JNIEnv* env, jclass, jstring filePath, jstring backupDir)
{
    JniStringUtfChars fpGuard(env, filePath);
    JniStringUtfChars bdGuard(env, backupDir);
    if (!fpGuard || !bdGuard) return JNI_FALSE;
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    bool ok = ravex::fileprot::backupFile(fpGuard.get(), bdGuard.get());
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_ravex_gui_fileprot_FileProtBridge_nativeRestoreFile(
    JNIEnv* env, jclass, jstring backupPath, jstring targetPath)
{
    JniStringUtfChars bpGuard(env, backupPath);
    JniStringUtfChars tpGuard(env, targetPath);
    if (!bpGuard || !tpGuard) return JNI_FALSE;
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    bool ok = ravex::fileprot::restoreFile(bpGuard.get(), tpGuard.get());
    if (ravex::jni::checkAndClearException(env)) return JNI_FALSE;
    return ok ? JNI_TRUE : JNI_FALSE;
}

}
