cmake_minimum_required(VERSION 3.14)

add_library(ravex_safewalk SHARED
    safewalk.cpp
    safewalk_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_safewalk PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_safewalk PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_safewalk")
endif()

target_include_directories(ravex_safewalk PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_safewalk PRIVATE ravex_common)

set_property(TARGET ravex_safewalk PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_safewalk PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_safewalk PRIVATE -O3 -march=native -ffast-math)
endif()

set_target_properties(ravex_safewalk PROPERTIES
    OUTPUT_NAME "ravex_safewalk"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_safewalk PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_safewalk PROPERTIES SUFFIX ".so")
endif()
