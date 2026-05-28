cmake_minimum_required(VERSION 3.16)

add_library(ravex_loader SHARED
    jni_loader.cpp
    checks.cpp
    optimize.cpp
)

target_include_directories(ravex_loader PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_loader PRIVATE
    pthread
    ${JNI_LIBRARIES}
)

# Output directly to resources for JAR bundling
set_target_properties(ravex_loader PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
