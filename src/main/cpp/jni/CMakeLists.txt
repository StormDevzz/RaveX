add_library(ravex_jni SHARED
    jni_bridge.cpp
)

target_link_libraries(ravex_jni PRIVATE ravex_common ${JNI_LIBRARIES})
target_include_directories(ravex_jni PRIVATE ${JNI_INCLUDE_DIRS})
