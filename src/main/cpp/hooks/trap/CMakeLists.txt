cmake_minimum_required(VERSION 3.14)

# Trap native calculation library
add_library(ravex_trap SHARED
    trap.cpp
    trap_jni.cpp
)

# JNI headers (required for JNI)
find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_trap PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_trap PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_trap")
endif()

target_include_directories(ravex_trap PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_trap PRIVATE ravex_common)

# C++17
set_property(TARGET ravex_trap PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_trap PROPERTY CXX_STANDARD_REQUIRED ON)

# Optimization options
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_trap PRIVATE -O3 -march=native -ffast-math)
endif()

# Output naming convention
set_target_properties(ravex_trap PROPERTIES
    OUTPUT_NAME "ravex_trap"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

# Platform-specific suffixes
if(WIN32)
    set_target_properties(ravex_trap PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_trap PROPERTIES SUFFIX ".so")
endif()
