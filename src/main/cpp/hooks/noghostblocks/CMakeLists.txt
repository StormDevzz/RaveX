cmake_minimum_required(VERSION 3.16)

add_library(ravex_noghostblocks SHARED
 
 
 
 
m
a
i
n
/
n
o
g
h
o
s
t
b
l
o
c
k
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
n
o
g
h
o
s
t
b
l
o
c
k
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

target_include_directories(ravex_noghostblocks PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_noghostblocks PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_noghostblocks PRIVATE pthread)
endif()

set_target_properties(ravex_noghostblocks PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_noghostblocks PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_noghostblocks PROPERTIES PREFIX "lib")
endif()
