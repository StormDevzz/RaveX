add_library(ravex_render SHARED
    trails_glow.cpp
)

target_include_directories(ravex_render PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

set_target_properties(ravex_render PROPERTIES
    OUTPUT_NAME "ravex_render"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_render PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_render PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_render PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_render PROPERTIES SUFFIX ".so")
endif()
