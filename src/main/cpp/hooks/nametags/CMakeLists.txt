cmake_minimum_required(VERSION 3.14)

# NameTags native calculation library
add_library(ravex_nametags SHARED
    nametags.cpp
    nametags_jni.cpp
)

# JNI headers (required for JNI)
find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_nametags PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_nametags PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_nametags")
endif()

target_include_directories(ravex_nametags PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_nametags PRIVATE ravex_common)

# C++17 or 23
set_property(TARGET ravex_nametags PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_nametags PROPERTY CXX_STANDARD_REQUIRED ON)

# Optimization options
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_nametags PRIVATE -O3 -march=native -ffast-math)
endif()

# Output naming convention
set_target_properties(ravex_nametags PROPERTIES
    OUTPUT_NAME "ravex_nametags"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

# Platform-specific suffixes
if(WIN32)
    set_target_properties(ravex_nametags PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_nametags PROPERTIES SUFFIX ".so")
endif()
