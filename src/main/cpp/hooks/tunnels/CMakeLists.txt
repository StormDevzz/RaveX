cmake_minimum_required(VERSION 3.16)

add_library(ravex_tunnels SHARED
 
 
 
 
m
a
i
n
/
t
u
n
n
e
l
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
t
u
n
n
e
l
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

target_include_directories(ravex_tunnels PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_tunnels PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_tunnels PRIVATE pthread)
endif()

set_target_properties(ravex_tunnels PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_tunnels PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_tunnels PROPERTIES PREFIX "lib")
endif()
