add_library(ravex_norender STATIC
    norender.cpp
)

target_link_libraries(ravex_norender PRIVATE ravex_common)
target_include_directories(ravex_norender PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)
