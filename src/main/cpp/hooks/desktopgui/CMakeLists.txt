cmake_minimum_required(VERSION 3.16)

add_library(ravex_desktopgui SHARED
    desktopgui_jni.cpp
    desktopgui.cpp
)

target_include_directories(ravex_desktopgui PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_desktopgui PRIVATE
    ravex_common
    ${JNI_LIBRARIES}
    pthread
)

if(WIN32)
    target_link_libraries(ravex_desktopgui PRIVATE user32 gdi32)
elseif(UNIX AND NOT APPLE)
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(GTK3 REQUIRED gtk+-3.0)
    target_include_directories(ravex_desktopgui PRIVATE ${GTK3_INCLUDE_DIRS})
    target_link_libraries(ravex_desktopgui PRIVATE ${GTK3_LIBRARIES})
endif()

# Output directly to resources for JAR bundling
set_target_properties(ravex_desktopgui PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
