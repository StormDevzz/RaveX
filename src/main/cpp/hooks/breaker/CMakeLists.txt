cmake_minimum_required(VERSION 3.14)

add_library(ravex_breaker SHARED
    main/breaker.cpp
    main/breaker_jni.cpp
    ../autocrystal/main/autocrystal.cpp
    ../autocrystal/damage/damage.cpp
    ../autocrystal/armor/armor.cpp
    ../autocrystal/effects/effects.cpp
    ../autocrystal/tracker/entity_tracker.cpp
    ../autocrystal/conditions/conditions.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_breaker PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_breaker PRIVATE ${JNI_LIBRARIES})
endif()

target_include_directories(ravex_breaker PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
    ${CMAKE_CURRENT_SOURCE_DIR}/../autocrystal/include
)

target_link_libraries(ravex_breaker PRIVATE ravex_common)

set_property(TARGET ravex_breaker PROPERTY CXX_STANDARD 17)
set_property(TARGET ravex_breaker PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_breaker PRIVATE /O2 /fp:fast)
    else()
<<<<<<< HEAD
        target_compile_options(ravex_breaker PRIVATE -O2)
=======
        target_compile_options(ravex_breaker PRIVATE -O3 -march=native -ffast-math)
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    endif()
endif()

set_target_properties(ravex_breaker PROPERTIES
    OUTPUT_NAME "ravex_breaker"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_breaker PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_breaker PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_breaker PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_breaker PROPERTIES SUFFIX ".so")
endif()
