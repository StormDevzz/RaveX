add_library(ravex_antiafk STATIC
    antiafk.cpp
)

target_link_libraries(ravex_antiafk PRIVATE ravex_common)
target_include_directories(ravex_antiafk PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

if(WIN32)
    target_sources(ravex_antiafk PRIVATE antiafk_windows.cpp)
elseif(UNIX AND NOT APPLE)
    target_sources(ravex_antiafk PRIVATE antiafk_linux.cpp)
    find_package(X11 REQUIRED)
    if(X11_FOUND)
        target_include_directories(ravex_antiafk PRIVATE ${X11_INCLUDE_DIR})
        target_link_libraries(ravex_antiafk PRIVATE ${X11_LIBRARIES} Xtst)
        target_compile_definitions(ravex_antiafk PRIVATE HAS_X11=1)
    endif()
else()
    message(WARNING "Unsupported platform for AntiAFK input backend")
endif()
