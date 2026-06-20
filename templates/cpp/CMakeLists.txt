cmake_minimum_required(VERSION 3.16)
project(another-addon VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(AnotherAddon SHARED
    AnotherAddon.cpp
)

target_include_directories(AnotherAddon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/main/cpp/addon/include
)

set_target_properties(AnotherAddon PROPERTIES
    OUTPUT_NAME "AnotherAddon"
    PREFIX ""
)

if(WIN32)
    set_target_properties(AnotherAddon PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(AnotherAddon PROPERTIES SUFFIX ".so")
endif()
