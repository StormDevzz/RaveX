cmake_minimum_required(VERSION 3.14)

add_library(ravex_packetmine SHARED
    main/packetmine.cpp
    main/packetmine_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_packetmine PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_packetmine PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_packetmine")
endif()

target_include_directories(ravex_packetmine PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_packetmine PRIVATE ravex_common)

set_property(TARGET ravex_packetmine PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_packetmine PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_packetmine PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_packetmine PRIVATE -O2)
    endif()
endif()

set_target_properties(ravex_packetmine PROPERTIES
    OUTPUT_NAME "ravex_packetmine"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_packetmine PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_packetmine PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_packetmine PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_packetmine PROPERTIES SUFFIX ".so")
endif()
