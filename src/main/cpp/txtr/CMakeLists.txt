cmake_minimum_required(VERSION 3.20)
project(txtr VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(txtr STATIC
    txtr.cpp
    format_png.cpp
    format_jpeg.cpp
    format_bmp.cpp
    format_tga.cpp
    format_dds.cpp
    converter.cpp
    resize.cpp
    optimize.cpp
)

target_include_directories(txtr PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_include_directories(txtr PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

find_package(PNG QUIET)
if(PNG_FOUND)
    target_include_directories(txtr PRIVATE ${PNG_INCLUDE_DIRS})
    target_link_libraries(txtr PRIVATE ${PNG_LIBRARIES})
    target_compile_definitions(txtr PRIVATE TXTR_HAVE_LIBPNG=1)
endif()

find_package(JPEG QUIET)
if(JPEG_FOUND)
    target_include_directories(txtr PRIVATE ${JPEG_INCLUDE_DIRS})
    target_link_libraries(txtr PRIVATE ${JPEG_LIBRARIES})
    target_compile_definitions(txtr PRIVATE TXTR_HAVE_LIBJPEG=1)
endif()

add_executable(txtr_cli txtr_cli.cpp)
target_link_libraries(txtr_cli PRIVATE txtr)

find_package(PkgConfig REQUIRED)
pkg_check_modules(GTK3 REQUIRED gtk+-3.0)

add_executable(txtr_gui txtr_gui.cpp)
target_include_directories(txtr_gui PRIVATE ${GTK3_INCLUDE_DIRS})
target_link_libraries(txtr_gui PRIVATE txtr ${GTK3_LIBRARIES})
target_compile_options(txtr_gui PRIVATE ${GTK3_CFLAGS_OTHER})
