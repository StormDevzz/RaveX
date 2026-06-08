cmake_minimum_required(VERSION 3.14)

# AutoCrystal native math library
add_library(ravex_autocrystal SHARED
    autocrystal.cpp
    autocrystal_jni.cpp
    damage.cpp
    armor.cpp
    effects.cpp
    entity_tracker.cpp
    conditions.cpp
)

# JNI headers (required for JNI)
find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_autocrystal PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_autocrystal PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_autocrystal")
endif()

target_include_directories(ravex_autocrystal PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_autocrystal PRIVATE ravex_common)

# C++17 for structured bindings and std::optional
set_property(TARGET ravex_autocrystal PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_autocrystal PROPERTY CXX_STANDARD_REQUIRED ON)

# Оптимизации
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_autocrystal PRIVATE -O3 -march=native -ffast-math)
endif()

# Output naming convention
set_target_properties(ravex_autocrystal PROPERTIES
    OUTPUT_NAME "ravex_autocrystal"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

# Platform-specific
if(WIN32)
    set_target_properties(ravex_autocrystal PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_autocrystal PROPERTIES SUFFIX ".so")
endif()
