cmake_minimum_required(VERSION 3.16)
project(MinimalCAddon LANGUAGES C)

set(CMAKE_C_STANDARD 23)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(c_addon_minimal SHARED src/addon.c)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(c_addon_minimal PRIVATE -O2)
endif()

set_target_properties(c_addon_minimal PROPERTIES
    PREFIX ""
    SUFFIX ".so"
    OUTPUT_NAME "c_addon_minimal"
)
