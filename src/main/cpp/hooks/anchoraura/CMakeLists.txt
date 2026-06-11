cmake_minimum_required(VERSION 3.14)

# AnchorAura native library
add_library(ravex_anchoraura SHARED
    anchoraura.cpp
    anchoraura_jni.cpp
    ../autocrystal/autocrystal.cpp
    ../autocrystal/damage.cpp
    ../autocrystal/armor.cpp
    ../autocrystal/effects.cpp
    ../autocrystal/entity_tracker.cpp
    ../autocrystal/conditions.cpp
)

# JNI headers (required for JNI)
find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_anchoraura PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_anchoraura PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_anchoraura")
endif()

target_include_directories(ravex_anchoraura PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}/../autocrystal
)

target_link_libraries(ravex_anchoraura PRIVATE ravex_common)

# C++17 standard
set_property(TARGET ravex_anchoraura PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_anchoraura PROPERTY CXX_STANDARD_REQUIRED ON)

# Optimization settings
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_anchoraura PRIVATE -O3 -march=native -ffast-math)
endif()

# Output properties
set_target_properties(ravex_anchoraura PROPERTIES
    OUTPUT_NAME "ravex_anchoraura"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

# Platform suffixes
if(WIN32)
    set_target_properties(ravex_anchoraura PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_anchoraura PROPERTIES SUFFIX ".so")
endif()
