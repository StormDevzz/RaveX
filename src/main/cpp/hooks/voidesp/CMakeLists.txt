cmake_minimum_required(VERSION 3.16)

add_library(ravex_voidesp SHARED
 
 
 
 
m
a
i
n
/
v
o
i
d
e
s
p
.
c
p
p


 
 
 
 
m
a
i
n
/
v
o
i
d
e
s
p
_
j
n
i
.
c
p
p
)

target_include_directories(ravex_voidesp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_voidesp PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_voidesp PRIVATE pthread)
endif()

set_target_properties(ravex_voidesp PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_voidesp PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_voidesp PROPERTIES PREFIX "lib")
endif()
