cmake_minimum_required(VERSION 3.16)

add_library(ravex_animations SHARED
    animations.cpp
    animations_jni.cpp
)

target_include_directories(ravex_animations PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_animations PRIVATE
    ${JNI_LIBRARIES}
    pthread
)

set_target_properties(ravex_animations PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
