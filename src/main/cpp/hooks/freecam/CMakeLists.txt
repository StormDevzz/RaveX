add_library(ravex_freecam STATIC
    main/freecam.cpp
    correction/camera_correction.cpp
)

target_link_libraries(ravex_freecam PRIVATE ravex_common)
target_include_directories(ravex_freecam PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
