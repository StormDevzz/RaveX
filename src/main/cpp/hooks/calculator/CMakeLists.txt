cmake_minimum_required(VERSION 3.16)

add_library(ravex_calculator SHARED
    calculator.cpp
    calculator_jni.cpp
)

target_include_directories(ravex_calculator PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

find_package(PkgConfig REQUIRED)
pkg_check_modules(GTK3 REQUIRED gtk+-3.0)
target_include_directories(ravex_calculator PRIVATE ${GTK3_INCLUDE_DIRS})
target_link_libraries(ravex_calculator PRIVATE
    ${JNI_LIBRARIES}
    ${GTK3_LIBRARIES}
    pthread
)

set_target_properties(ravex_calculator PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
