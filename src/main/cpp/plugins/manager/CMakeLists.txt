add_library(ravex_manager SHARED
    manager.cpp
)

target_include_directories(ravex_manager PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

set_target_properties(ravex_manager PROPERTIES
    OUTPUT_NAME "ravex_manager"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_manager PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_manager PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_manager PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_manager PROPERTIES SUFFIX ".so")
endif()
