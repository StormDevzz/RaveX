cmake_minimum_required(VERSION 3.14)

if(WIN32)
    add_library(ravex_desktopgui SHARED
        main/desktopgui.cpp
        main/desktopgui_windows.cpp
    )
else()
    add_library(ravex_desktopgui SHARED
        main/desktopgui.cpp
        main/desktopgui_jni.cpp
    )
endif()

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_desktopgui PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_desktopgui PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_desktopgui")
endif()

target_include_directories(ravex_desktopgui PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_desktopgui PRIVATE ravex_common)

set_property(TARGET ravex_desktopgui PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_desktopgui PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_desktopgui PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_desktopgui PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_desktopgui PROPERTIES
    OUTPUT_NAME "ravex_desktopgui"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_desktopgui PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_desktopgui PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_desktopgui PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_desktopgui PROPERTIES SUFFIX ".so")
endif()
