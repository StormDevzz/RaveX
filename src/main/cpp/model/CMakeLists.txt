cmake_minimum_required(VERSION 3.20)
project(model VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(model STATIC
    model.cpp
    format_json.cpp
    format_bbmodel.cpp
    format_gltf.cpp
    format_obj.cpp
    format_pmx.cpp
    converter.cpp
    optimize.cpp
    anim.cpp
)

target_include_directories(model PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

add_executable(model_cli model_cli.cpp)
target_link_libraries(model_cli PRIVATE model)
