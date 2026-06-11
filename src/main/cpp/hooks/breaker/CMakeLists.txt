cmake_minimum_required(VERSION 3.14)

add_library(ravex_breaker SHARED
    breaker.cpp
    breaker_jni.cpp
    ../autocrystal/autocrystal.cpp
    ../autocrystal/damage.cpp
    ../autocrystal/armor.cpp
    ../autocrystal/effects.cpp
    ../autocrystal/entity_tracker.cpp
    ../autocrystal/conditions.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_breaker PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_breaker PRIVATE ${JNI_LIBRARIES})
endif()

target_include_directories(ravex_breaker PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}/../autocrystal
)

target_link_libraries(ravex_breaker PRIVATE ravex_common)

set_property(TARGET ravex_breaker PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_breaker PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_breaker PRIVATE -O3 -march=native -ffast-math)
endif()

set_target_properties(ravex_breaker PROPERTIES
    OUTPUT_NAME "ravex_breaker"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_breaker PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_breaker PROPERTIES SUFFIX ".so")
endif()
