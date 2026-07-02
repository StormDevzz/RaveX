#pragma once
#include <jni.h>

namespace ravex {
namespace addon {

class AddonJni {
public:
    static void registerNatives(JNIEnv* env);
};

}
}
