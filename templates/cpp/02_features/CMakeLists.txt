cmake_minimum_required(VERSION 3.16)
project(FeatureAddon VERSION 1.4.3 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(WIN32)
    if(MSVC)
        add_link_options(/MT)
    else()
        add_link_options(-static -static-libgcc -static-libstdc++)
    endif()
endif()

if(MSVC)
    add_compile_options(/O2 /fp:fast /GL /utf-8)
else()
    add_compile_options(-O3 -march=native -flto -fvisibility=hidden)
    add_link_options(-flto)
endif()

add_library(FeatureAddon SHARED
    main.cpp
    JniBridge.cpp
)

target_include_directories(FeatureAddon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/main/cpp/addon/include
)

if(WIN32)
    target_link_libraries(FeatureAddon PRIVATE winmm psapi)
else()
    find_package(Threads REQUIRED)
    target_link_libraries(FeatureAddon PRIVATE Threads::Threads)
endif()

set_target_properties(FeatureAddon PROPERTIES
    OUTPUT_NAME "FeatureAddon"
)

if(WIN32)
    set_target_properties(FeatureAddon PROPERTIES PREFIX "" SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(FeatureAddon PROPERTIES PREFIX "lib" SUFFIX ".so")
endif()
