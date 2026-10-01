cmake_minimum_required(VERSION 3.14)

add_library(ravex_font SHARED
    main/font_sdf.cpp
    main/font_sdf_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_font PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_font PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found - cannot build ravex_font")
endif()

target_include_directories(ravex_font PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

set_property(TARGET ravex_font PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_font PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_font PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_font PRIVATE -O2)
    endif()
endif()

set_target_properties(ravex_font PROPERTIES
    OUTPUT_NAME "ravex_font"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_font PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_font PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_font PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_font PROPERTIES SUFFIX ".so")
endif()
