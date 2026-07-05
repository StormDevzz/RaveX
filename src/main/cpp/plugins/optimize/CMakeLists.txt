add_library(ravex_optimize SHARED
    optimize.cpp
)

target_include_directories(ravex_optimize PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

if(WIN32)
    add_subdirectory(windows)
    target_include_directories(ravex_optimize PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include/windows)
    target_link_libraries(ravex_optimize PRIVATE ravex_optimize_windows)
endif()

set_target_properties(ravex_optimize PROPERTIES
    OUTPUT_NAME "ravex_optimize"
    WINDOWS_EXPORT_ALL_SYMBOLS ON
)

if(WIN32)
    set_target_properties(ravex_optimize PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_optimize PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_optimize PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_optimize PROPERTIES SUFFIX ".so")
endif()
