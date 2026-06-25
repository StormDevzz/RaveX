cmake_minimum_required(VERSION 3.16)
project(OverlayAddon VERSION 1.4.3 LANGUAGES CXX)

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

add_library(OverlayAddon SHARED main.cpp)

target_include_directories(OverlayAddon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/main/cpp/addon/include
)

if(WIN32)
    target_link_libraries(OverlayAddon PRIVATE gdi32 dwmapi)
else()
    find_package(X11 REQUIRED)
    target_include_directories(OverlayAddon PRIVATE ${X11_INCLUDE_DIR})
    target_link_libraries(OverlayAddon PRIVATE ${X11_LIBRARIES})
endif()

set_target_properties(OverlayAddon PROPERTIES
    OUTPUT_NAME "OverlayAddon"
)

if(WIN32)
    set_target_properties(OverlayAddon PROPERTIES PREFIX "" SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(OverlayAddon PROPERTIES PREFIX "lib" SUFFIX ".so")
endif()
