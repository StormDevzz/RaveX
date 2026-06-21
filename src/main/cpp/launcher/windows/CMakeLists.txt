cmake_minimum_required(VERSION 3.16)

add_executable(ravex_launcher
    main.cpp
)

target_include_directories(ravex_launcher PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

if(WIN32)
    set_target_properties(ravex_launcher PROPERTIES SUFFIX ".exe")
endif()
