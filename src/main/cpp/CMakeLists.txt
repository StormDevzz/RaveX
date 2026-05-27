cmake_minimum_required(VERSION 3.16)
project(ravex-native VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)

add_subdirectory(common)
add_subdirectory(optimizer)
add_subdirectory(jni)
