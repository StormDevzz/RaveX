cmake_minimum_required(VERSION 3.14)

add_library(ravex_tntaura SHARED
    tntaura.cpp
    pathfinder.cpp
    damage_calc.cpp
    tntaura_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_tntaura PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_tntaura PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_tntaura")
endif()

target_include_directories(ravex_tntaura PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_tntaura PRIVATE ravex_common)

set_property(TARGET ravex_tntaura PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_tntaura PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_tntaura PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_tntaura PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_tntaura PROPERTIES
    OUTPUT_NAME "ravex_tntaura"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_tntaura PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_tntaura PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_tntaura PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_tntaura PROPERTIES SUFFIX ".so")
endif()
