cmake_minimum_required(VERSION 3.14)

add_library(ravex_ecfarmer SHARED
    main/ecfarmer.cpp
    main/ecfarmer_jni.cpp
    break/ecfarmer_break.cpp
    place/ecfarmer_place.cpp
    rotation/ecfarmer_rotation.cpp
    scan/ecfarmer_scan.cpp
    util/ecfarmer_util.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_ecfarmer PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_ecfarmer PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_ecfarmer")
endif()

target_include_directories(ravex_ecfarmer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(ravex_ecfarmer PRIVATE ravex_common)

set_property(TARGET ravex_ecfarmer PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_ecfarmer PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_ecfarmer PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_ecfarmer PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_ecfarmer PROPERTIES
    OUTPUT_NAME "ravex_ecfarmer"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_ecfarmer PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_ecfarmer PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_ecfarmer PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_ecfarmer PROPERTIES SUFFIX ".so")
endif()
