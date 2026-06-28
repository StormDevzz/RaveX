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
    ravex_github_tools
    ravex_brand
    ${JNI_LIBRARIES}
)

if(TARGET ravex_lua)
    target_link_libraries(ravex_jni PRIVATE ravex_lua)
endif()

target_include_directories(ravex_jni PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_SOURCE_DIR}/antiafk
    ${CMAKE_SOURCE_DIR}/hooks/shaders
    ${CMAKE_SOURCE_DIR}/hooks/norender
    ${CMAKE_SOURCE_DIR}/plugins/optimize
    ${CMAKE_SOURCE_DIR}/plugins/manager
    ${CMAKE_SOURCE_DIR}/plugins/github/include
    ${CMAKE_SOURCE_DIR}/plugins/brand/include
)

if(WIN32)
    target_link_libraries(ravex_jni PRIVATE winmm ravex_optimize_windows)
    target_include_directories(ravex_jni PRIVATE ${CMAKE_SOURCE_DIR}/plugins/optimize/windows)
endif()

if(NOT WIN32)
    find_package(X11 REQUIRED)
    if(X11_FOUND)
        target_include_directories(ravex_jni PRIVATE ${X11_INCLUDE_DIR})
        target_link_libraries(ravex_jni PRIVATE ${X11_LIBRARIES} Xtst)
    endif()
endif()

set_target_properties(ravex_jni PROPERTIES
    OUTPUT_NAME "ravex_jni"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_jni PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_jni PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_jni PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_jni PROPERTIES SUFFIX ".so")
endif()
