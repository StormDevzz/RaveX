add_executable(ravex_optimizer
    main.cpp
    optimizer.cpp
)

target_link_libraries(ravex_optimizer PRIVATE ravex_common)
target_include_directories(ravex_optimizer PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)
