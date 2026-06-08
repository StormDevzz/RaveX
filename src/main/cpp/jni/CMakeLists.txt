add_library(ravex_jni SHARED
    jni_bridge.cpp
)

target_link_libraries(ravex_jni PRIVATE
    ravex_common
    ravex_antiafk
    ravex_shaders
    ravex_norender
    ${JNI_LIBRARIES}
)

target_include_directories(ravex_jni PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_SOURCE_DIR}/antiafk
    ${CMAKE_SOURCE_DIR}/hooks/shaders
    ${CMAKE_SOURCE_DIR}/hooks/norender
)

if(WIN32)
    # Windows: link nothing extra
elseif(UNIX AND NOT APPLE)
    find_package(X11 REQUIRED)
    if(X11_FOUND)
        target_include_directories(ravex_jni PRIVATE ${X11_INCLUDE_DIR})
        target_link_libraries(ravex_jni PRIVATE ${X11_LIBRARIES} Xtst)
    endif()
endif()
