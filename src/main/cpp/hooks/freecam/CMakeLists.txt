add_library(ravex_freecam STATIC
    freecam.cpp
    camera_correction.cpp
)

target_link_libraries(ravex_freecam PRIVATE ravex_common)
target_include_directories(ravex_freecam PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
