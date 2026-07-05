if(WIN32)
    add_library(ravex_optimize_windows STATIC
        startup_boost.cpp
        memory_boost.cpp
        system_config.cpp
    )

    target_include_directories(ravex_optimize_windows PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/plugins/optimize
    )

    target_link_libraries(ravex_optimize_windows PUBLIC
        avrt winmm powrprof ws2_32
    )
endif()
