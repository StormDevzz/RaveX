cmake_minimum_required(VERSION 3.16)

add_library(ravex_holefill SHARED
 
 
 
 
m
a
i
n
/
h
o
l
e
f
i
l
l
.
c
p
p


 
 
 
 
m
a
i
n
/
h
o
l
e
f
i
l
l
_
j
n
i
.
c
p
p
)

target_include_directories(ravex_holefill PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_holefill PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_holefill PRIVATE pthread)
endif()

set_target_properties(ravex_holefill PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_holefill PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_holefill PROPERTIES PREFIX "lib")
endif()
