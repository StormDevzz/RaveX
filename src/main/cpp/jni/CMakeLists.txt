add_library(ravex_jni SHARED
    jni_bridge.cpp
)

target_link_libraries(ravex_jni PRIVATE
    ravex_common
    ravex_antiafk
    ravex_shaders
    ravex_norender
    ravex_optimize
    ravex_manager
    ${JNI_LIBRARIES}
)

target_include_directories(ravex_jni PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_SOURCE_DIR}/antiafk
    ${CMAKE_SOURCE_DIR}/hooks/shaders
    ${CMAKE_SOURCE_DIR}/hooks/norender
    ${CMAKE_SOURCE_DIR}/plugins/optimize
    ${CMAKE_SOURCE_DIR}/plugins/manager
)

if(WIN32)
    target_link_libraries(ravex_jni PRIVATE winmm)
elseif(UNIX AND NOT APPLE)
    find_package(X11 REQUIRED)
    if(X11_FOUND)
        target_include_directories(ravex_jni PRIVATE ${X11_INCLUDE_DIR})
        target_link_libraries(ravex_jni PRIVATE ${X11_LIBRARIES} Xtst)
    endif()
endif()

# Output naming convention and destination
set_target_properties(ravex_jni PROPERTIES
    OUTPUT_NAME "ravex_jni"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
