add_library(ravex_manager STATIC
    manager.cpp
)

target_include_directories(ravex_manager PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
