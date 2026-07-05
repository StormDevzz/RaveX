cmake_minimum_required(VERSION 3.16)

add_library(ravex_animations SHARED
 
 
 
 
m
a
i
n
/
a
n
i
m
a
t
i
o
n
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
a
n
i
m
a
t
i
o
n
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

target_include_directories(ravex_animations PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_animations PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_animations PRIVATE pthread)
endif()

set_target_properties(ravex_animations PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_animations PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_animations PROPERTIES PREFIX "lib")
endif()
