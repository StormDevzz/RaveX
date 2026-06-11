cmake_minimum_required(VERSION 3.14)

# BasePlace native library
add_library(ravex_baseplace SHARED
    baseplace.cpp
    baseplace_jni.cpp
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
    target_include_directories(ravex_baseplace PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_baseplace PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_baseplace")
endif()

target_include_directories(ravex_baseplace PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}/../autocrystal
)

target_link_libraries(ravex_baseplace PRIVATE ravex_common)

# C++17 standard
set_property(TARGET ravex_baseplace PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_baseplace PROPERTY CXX_STANDARD_REQUIRED ON)

# Optimization settings
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_baseplace PRIVATE -O3 -march=native -ffast-math)
endif()

# Output naming and location properties
set_target_properties(ravex_baseplace PROPERTIES
    OUTPUT_NAME "ravex_baseplace"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

# Platform-specific suffixes
if(WIN32)
    set_target_properties(ravex_baseplace PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_baseplace PROPERTIES SUFFIX ".so")
endif()
