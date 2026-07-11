cmake_minimum_required(VERSION 3.14)

add_library(ravex_shieldfucker SHARED
    main/shieldfucker.cpp
    main/shieldfucker_jni.cpp
    target/shieldfucker_target.cpp
    math/shieldfucker_math.cpp
    platform/shieldfucker_platform.cpp
    tracker/shieldfucker_tracker.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_shieldfucker PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_shieldfucker PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_shieldfucker")
endif()

target_include_directories(ravex_shieldfucker PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

set_property(TARGET ravex_shieldfucker PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_shieldfucker PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_shieldfucker PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_shieldfucker PRIVATE -O2)
    endif()
endif()

set_target_properties(ravex_shieldfucker PROPERTIES
    OUTPUT_NAME "ravex_shieldfucker"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_shieldfucker PROPERTIES PREFIX "")
    set_target_properties(ravex_shieldfucker PROPERTIES SUFFIX ".dll")
else()
    set_target_properties(ravex_shieldfucker PROPERTIES PREFIX "lib")
    set_target_properties(ravex_shieldfucker PROPERTIES SUFFIX ".so")
endif()

if(MSVC)
    set_target_properties(ravex_shieldfucker PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS TRUE)
endif()
