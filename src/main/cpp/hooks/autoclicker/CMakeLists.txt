cmake_minimum_required(VERSION 3.16)

add_library(ravex_autoclicker SHARED
 
 
 
 
m
a
i
n
/
a
u
t
o
c
l
i
c
k
e
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
u
t
o
c
l
i
c
k
e
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

target_include_directories(ravex_autoclicker PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_autoclicker PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_autoclicker PRIVATE pthread)
endif()

set_target_properties(ravex_autoclicker PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_autoclicker PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_autoclicker PROPERTIES PREFIX "lib")
endif()
