add_library(ravex_shaders STATIC
    native/jni_shader_math.cpp
)

target_include_directories(ravex_shaders PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/hand/include
)
