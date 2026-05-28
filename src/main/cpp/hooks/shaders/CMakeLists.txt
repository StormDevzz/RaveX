add_library(ravex_shaders STATIC
    color_shader.cpp
)

target_include_directories(ravex_shaders PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
