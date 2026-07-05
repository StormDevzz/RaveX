add_library(ravex_norender STATIC
    main/norender.cpp
)

target_link_libraries(ravex_norender PRIVATE ravex_common)
target_include_directories(ravex_norender PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)
