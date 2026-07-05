cmake_minimum_required(VERSION 3.14)

add_library(ravex_elytraplusplus SHARED
    main/elytraplusplus.cpp
    main/elytraplusplus_jni.cpp
    bypass/bypass_handler.cpp
    grim/grim_bypass.cpp
    velocity/velocity_math.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_elytraplusplus PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_elytraplusplus PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_elytraplusplus")
endif()

target_include_directories(ravex_elytraplusplus PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_elytraplusplus PRIVATE ravex_common)

set_property(TARGET ravex_elytraplusplus PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_elytraplusplus PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_elytraplusplus PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_elytraplusplus PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_elytraplusplus PROPERTIES
    OUTPUT_NAME "ravex_elytraplusplus"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_elytraplusplus PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_elytraplusplus PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_elytraplusplus PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_elytraplusplus PROPERTIES SUFFIX ".so")
endif()
