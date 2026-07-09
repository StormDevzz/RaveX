cmake_minimum_required(VERSION 3.14)

add_library(ravex_nuker SHARED
    main/nuker.cpp
    main/nuker_jni.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_nuker PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_nuker PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_nuker")
endif()

target_include_directories(ravex_nuker PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_nuker PRIVATE ravex_common)

set_property(TARGET ravex_nuker PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_nuker PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_nuker PRIVATE /O2 /fp:fast)
    else()
<<<<<<< HEAD
        target_compile_options(ravex_nuker PRIVATE -O2)
=======
        target_compile_options(ravex_nuker PRIVATE -O3 -march=native -ffast-math)
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    endif()
endif()

set_target_properties(ravex_nuker PROPERTIES
    OUTPUT_NAME "ravex_nuker"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_nuker PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_nuker PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_nuker PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_nuker PROPERTIES SUFFIX ".so")
endif()
