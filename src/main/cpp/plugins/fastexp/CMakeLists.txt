cmake_minimum_required(VERSION 3.16)

add_library(ravex_fastexp SHARED
    fastexp.cpp
    fastexp_jni.cpp
)

target_include_directories(ravex_fastexp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_fastexp PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_fastexp PRIVATE pthread)
endif()

set_target_properties(ravex_fastexp PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_fastexp PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_fastexp PROPERTIES PREFIX "lib")
endif()
