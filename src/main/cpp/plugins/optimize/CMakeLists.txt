add_library(ravex_optimize STATIC
    optimize.cpp
)

target_include_directories(ravex_optimize PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
