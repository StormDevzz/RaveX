cmake_minimum_required(VERSION 3.16)

add_library(ravex_antibot SHARED
 
 
 
 
m
a
i
n
/
a
n
t
i
b
o
t
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
b
o
t
_
j
n
i
.
c
p
p
)

target_include_directories(ravex_antibot PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_antibot PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_antibot PRIVATE pthread)
endif()

set_target_properties(ravex_antibot PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_antibot PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_antibot PROPERTIES PREFIX "lib")
endif()
