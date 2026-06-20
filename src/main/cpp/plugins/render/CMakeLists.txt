add_library(ravex_render STATIC
    trails_glow.cpp
)

target_include_directories(ravex_render PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
