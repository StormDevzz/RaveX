cmake_minimum_required(VERSION 3.14)

add_library(ravex_baseplace SHARED
    baseplace.cpp
    baseplace_jni.cpp
    ../autocrystal/main/autocrystal.cpp
    ../autocrystal/damage/damage.cpp
    ../autocrystal/armor/armor.cpp
    ../autocrystal/effects/effects.cpp
    ../autocrystal/tracker/entity_tracker.cpp
    ../autocrystal/conditions/conditions.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_baseplace PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_baseplace PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_baseplace")
endif()

target_include_directories(ravex_baseplace PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}/../autocrystal/include
)

target_link_libraries(ravex_baseplace PRIVATE ravex_common)

set_property(TARGET ravex_baseplace PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_baseplace PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_baseplace PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_baseplace PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_baseplace PROPERTIES
    OUTPUT_NAME "ravex_baseplace"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_baseplace PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_baseplace PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_baseplace PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_baseplace PROPERTIES SUFFIX ".so")
endif()
