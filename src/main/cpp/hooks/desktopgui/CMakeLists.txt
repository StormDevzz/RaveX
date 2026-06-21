cmake_minimum_required(VERSION 3.16)

if(WIN32)
    add_library(ravex_desktopgui SHARED
        desktopgui_windows.cpp
    )
else()
    add_library(ravex_desktopgui SHARED
        desktopgui_jni.cpp
    )
endif()

target_include_directories(ravex_desktopgui PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_desktopgui PRIVATE
    ravex_common
    ${JNI_LIBRARIES}
)

if(WIN32)
    target_link_libraries(ravex_desktopgui PRIVATE comctl32)
endif()

if(NOT WIN32)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(GTK3 REQUIRED gtk+-3.0)
    target_include_directories(ravex_desktopgui PRIVATE ${GTK3_INCLUDE_DIRS})
    target_link_libraries(ravex_desktopgui PRIVATE ${GTK3_LIBRARIES})
    target_link_libraries(ravex_desktopgui PRIVATE pthread)
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
