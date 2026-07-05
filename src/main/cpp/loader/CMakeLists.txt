cmake_minimum_required(VERSION 3.16)

add_library(ravex_loader SHARED
    jni_loader.cpp
    checks.cpp
    optimize.cpp
)

target_include_directories(ravex_loader PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_loader PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_loader PRIVATE pthread)
endif()

if(WIN32)
    target_link_libraries(ravex_loader PRIVATE psapi.lib)
endif()

set_target_properties(ravex_loader PROPERTIES
    OUTPUT_NAME "ravex_loader"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_loader PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_loader PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_loader PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_loader PROPERTIES SUFFIX ".so")
endif()
