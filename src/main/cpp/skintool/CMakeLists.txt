cmake_minimum_required(VERSION 3.20)
project(skintool VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 REQUIRED COMPONENTS Widgets OpenGLWidgets)
find_package(OpenGL REQUIRED)
find_package(GLEW REQUIRED)

add_executable(skintool
    misc/main.cpp
    include/skin_window.hpp
    display/main_window.cpp
    display/skin_renderer.cpp
    camera/camera_orbit.cpp
    camera/camera_matrix.cpp
    camera/camera_state.cpp
    shader/shader_source.cpp
    shader/shader_compile.cpp
    shader/shader_uniform.cpp
    texture/texture_load.cpp
    texture/texture_render.cpp
    texture/texture_convert.cpp
    model/skin_model_build.cpp
    model/skin_model_draw.cpp
    model/skin_model_part.cpp
)

target_include_directories(skintool PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${GLEW_INCLUDE_DIRS}
)

target_link_libraries(skintool PRIVATE
    Qt6::Widgets
    Qt6::OpenGLWidgets
    ${OPENGL_gl_LIBRARY}
    ${GLEW_LIBRARIES}
    dl
    pthread
)


