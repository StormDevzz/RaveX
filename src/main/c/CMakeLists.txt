cmake_minimum_required(VERSION 3.16)
project(ravex-c-native VERSION 1.0.0 LANGUAGES C)

set(CMAKE_C_STANDARD 23)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(WIN32)
    return()
endif()

add_link_options(-static -static-libgcc)

find_package(JNI REQUIRED)

add_subdirectory(common)

function(add_ravex_c_module name)
    cmake_parse_arguments(MOD "" "" "SOURCES;INCLUDES;LIBS" ${ARGN})
    add_library(${name} SHARED ${MOD_SOURCES})
    target_include_directories(${name} PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(${name} PRIVATE ${JNI_LIBRARIES})
    target_include_directories(${name} PRIVATE
        ${CMAKE_SOURCE_DIR}/common
        ${MOD_INCLUDES}
    )
    target_link_libraries(${name} PRIVATE ravex_c_common ${MOD_LIBS})
    set_property(TARGET ${name} PROPERTY C_STANDARD 23)
    set_property(TARGET ${name} PROPERTY C_STANDARD_REQUIRED ON)
    if(CMAKE_BUILD_TYPE STREQUAL "Release")
        target_compile_options(${name} PRIVATE -O2)
    endif()
    set_target_properties(${name} PROPERTIES
        OUTPUT_NAME "ravex_${name}"
        PREFIX "lib"
        SUFFIX ".so"
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
    )
endfunction()
