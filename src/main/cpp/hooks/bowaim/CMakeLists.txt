cmake_minimum_required(VERSION 3.14)

add_library(ravex_bowaim SHARED
    bowaim.cpp
    bowaim_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_bowaim PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_bowaim PRIVATE ${JNI_LIBRARIES})
endif()

target_include_directories(ravex_bowaim PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_bowaim PRIVATE ravex_common)

set_property(TARGET ravex_bowaim PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_bowaim PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_bowaim PRIVATE -O3 -march=native -ffast-math)
endif()

set_target_properties(ravex_bowaim PROPERTIES
    OUTPUT_NAME "ravex_bowaim"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_bowaim PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_bowaim PROPERTIES SUFFIX ".so")
endif()
