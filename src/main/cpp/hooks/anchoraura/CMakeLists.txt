cmake_minimum_required(VERSION 3.14)

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

set_property(TARGET ravex_anchoraura PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_anchoraura PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_anchoraura PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_anchoraura PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_anchoraura PROPERTIES
    OUTPUT_NAME "ravex_anchoraura"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_anchoraura PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_anchoraura PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_anchoraura PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_anchoraura PROPERTIES SUFFIX ".so")
endif()
