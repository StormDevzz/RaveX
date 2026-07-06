add_library(ravex_common STATIC
    memory.cpp
    background_remover.cpp
)

target_include_directories(ravex_common PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

if(WIN32)
    target_link_libraries(ravex_common PRIVATE psapi.lib)
endif()
