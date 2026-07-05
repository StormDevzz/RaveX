cmake_minimum_required(VERSION 3.14)

add_library(ravex_faststairs SHARED
 
 
 
 
m
a
i
n
/
f
a
s
t
s
t
a
i
r
s
.
c
p
p


 
 
 
 
m
a
i
n
/
f
a
s
t
s
t
a
i
r
s
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
    target_include_directories(ravex_faststairs PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_faststairs PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_faststairs")
endif()

target_include_directories(ravex_faststairs PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_faststairs PRIVATE ravex_common)

set_property(TARGET ravex_faststairs PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_faststairs PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_faststairs PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_faststairs PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_faststairs PROPERTIES
    OUTPUT_NAME "ravex_faststairs"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_faststairs PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_faststairs PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_faststairs PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_faststairs PROPERTIES SUFFIX ".so")
endif()
