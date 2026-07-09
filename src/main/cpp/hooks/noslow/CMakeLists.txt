cmake_minimum_required(VERSION 3.14)

add_library(ravex_noslow SHARED
    main/noslow.cpp
    main/noslow_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_noslow PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_noslow PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_noslow")
endif()

target_include_directories(ravex_noslow PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_noslow PRIVATE ravex_common)

set_property(TARGET ravex_noslow PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_noslow PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_noslow PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_noslow PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_noslow PROPERTIES
    OUTPUT_NAME "ravex_noslow"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_noslow PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_noslow PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_noslow PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_noslow PROPERTIES SUFFIX ".so")
endif()
