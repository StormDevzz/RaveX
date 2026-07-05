cmake_minimum_required(VERSION 3.16)

add_library(ravex_packetmine SHARED
 
 
 
 
m
a
i
n
/
p
a
c
k
e
t
m
i
n
e
.
c
p
p


 
 
 
 
m
a
i
n
/
p
a
c
k
e
t
m
i
n
e
_
j
n
i
.
c
p
p
)

target_include_directories(ravex_packetmine PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_packetmine PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_packetmine PRIVATE pthread)
endif()

set_target_properties(ravex_packetmine PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_packetmine PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_packetmine PROPERTIES PREFIX "lib")
endif()
