cmake_minimum_required(VERSION 3.14)

add_library(ravex_antiregear SHARED
 
 
 
 
m
a
i
n
/
a
n
t
i
r
e
g
e
a
r
.
c
p
p


 
 
 
 
m
a
i
n
/
a
n
t
i
r
e
g
e
a
r
_
j
n
i
.
c
p
p
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_antiregear PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_antiregear PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_antiregear")
endif()

target_include_directories(ravex_antiregear PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_antiregear PRIVATE ravex_common)

set_property(TARGET ravex_antiregear PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_antiregear PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_antiregear PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_antiregear PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_antiregear PROPERTIES
    OUTPUT_NAME "ravex_antiregear"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_antiregear PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_antiregear PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_antiregear PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_antiregear PROPERTIES SUFFIX ".so")
endif()
