cmake_minimum_required(VERSION 3.16)

add_library(ravex_holefill SHARED
    holefill.cpp
    holefill_jni.cpp
)

target_include_directories(ravex_holefill PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_holefill PRIVATE
    ${JNI_LIBRARIES}
    pthread
)

set_target_properties(ravex_holefill PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
