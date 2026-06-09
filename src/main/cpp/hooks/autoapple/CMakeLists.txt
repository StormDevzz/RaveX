cmake_minimum_required(VERSION 3.14)

# AutoApple native library
add_library(ravex_autoapple SHARED
    autoapple.cpp
    autoapple_jni.cpp
)

# JNI headers (required for JNI)
find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_autoapple PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_autoapple PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_autoapple")
endif()

target_include_directories(ravex_autoapple PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_autoapple PRIVATE ravex_common)

# C++17 or 23
set_property(TARGET ravex_autoapple PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_autoapple PROPERTY CXX_STANDARD_REQUIRED ON)

# Optimization options
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_autoapple PRIVATE -O3 -march=native -ffast-math)
endif()

# Output naming convention
set_target_properties(ravex_autoapple PROPERTIES
    OUTPUT_NAME "ravex_autoapple"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

# Platform-specific suffixes
if(WIN32)
    set_target_properties(ravex_autoapple PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_autoapple PROPERTIES SUFFIX ".so")
endif()
