#include <jni.h>
#include "noghostblocks.h"
#include <string>



extern "C" {

JNIEXPORT void JNICALL
Java_ravex_modules_player_NoGhostBlocks_nativeServerBlockUpdate(
    JNIEnv* env, jclass cls,
    jint x, jint y, jint z, jstring blockId)
{
    const char* bid = env->GetStringUTFChars(blockId, nullptr);
    ravex::NoGhostBlocksEngine::instance().onServerBlockUpdate(x, y, z, std::string(bid));
    env->ReleaseStringUTFChars(blockId, bid);
}

JNIEXPORT jboolean JNICALL
Java_ravex_modules_player_NoGhostBlocks_nativeIsGhostBlock(
    JNIEnv* env, jclass cls,
    jint x, jint y, jint z, jstring clientBlockId)
{
    const char* cid = env->GetStringUTFChars(clientBlockId, nullptr);
    bool result = ravex::NoGhostBlocksEngine::instance().isGhostBlock(x, y, z, std::string(cid));
    env->ReleaseStringUTFChars(clientBlockId, cid);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_NoGhostBlocks_nativeMarkMiningStart(
    JNIEnv* env, jclass cls, jint x, jint y, jint z)
{
    ravex::NoGhostBlocksEngine::instance().markMiningStart(x, y, z);
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_NoGhostBlocks_nativeMarkMiningEnd(
    JNIEnv* env, jclass cls, jint x, jint y, jint z)
{
    ravex::NoGhostBlocksEngine::instance().markMiningEnd(x, y, z);
}

JNIEXPORT void JNICALL
Java_ravex_modules_player_NoGhostBlocks_nativeReset(
    JNIEnv* env, jclass cls)
{
    ravex::NoGhostBlocksEngine::instance().reset();
}

} 
