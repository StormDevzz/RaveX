cmake_minimum_required(VERSION 3.16)

add_library(ravex_nativesc SHARED
    nativesc.cpp
    nativesc_jni.cpp
    nativesc_buffer.cpp
    nativesc_capture.cpp
    nativesc_compress.cpp
    nativesc_linux.cpp
    nativesc_region.cpp
    nativesc_save.cpp
)

if(WIN32)
    target_sources(ravex_nativesc PRIVATE nativesc_win.cpp)
endif()

target_include_directories(ravex_nativesc PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_nativesc PRIVATE ${JNI_LIBRARIES})

if(NOT WIN32)
    target_link_libraries(ravex_nativesc PRIVATE pthread X11 Xext)
    find_package(X11 REQUIRED)
    target_include_directories(ravex_nativesc PRIVATE ${X11_INCLUDE_DIR})
endif()

set_target_properties(ravex_nativesc PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_nativesc PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_nativesc PROPERTIES PREFIX "lib")
endif()
