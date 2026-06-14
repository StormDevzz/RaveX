add_library(ravex_common STATIC
    memory.cpp
    background_remover.cpp
)

target_include_directories(ravex_common PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
