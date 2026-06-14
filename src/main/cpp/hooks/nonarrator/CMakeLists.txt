cmake_minimum_required(VERSION 3.14)

add_library(ravex_nonarrator SHARED
    nonarrator.cpp
    nonarrator_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_nonarrator PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_nonarrator PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_nonarrator")
endif()

target_include_directories(ravex_nonarrator PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_nonarrator PRIVATE ravex_common)

set_property(TARGET ravex_nonarrator PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_nonarrator PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_nonarrator PRIVATE -O3 -march=native -ffast-math)
endif()

set_target_properties(ravex_nonarrator PROPERTIES
    OUTPUT_NAME "ravex_nonarrator"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_nonarrator PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_nonarrator PROPERTIES SUFFIX ".so")
endif()
