cmake_minimum_required(VERSION 3.16)
project(ravex-native VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(WIN32)
    add_link_options(-static -static-libgcc -static-libstdc++)
endif()

find_package(JNI REQUIRED)

add_subdirectory(common)
add_subdirectory(antiafk)
add_subdirectory(hooks/shaders)
add_subdirectory(hooks/norender)
add_subdirectory(jni)
add_subdirectory(hooks/autocrystal)
