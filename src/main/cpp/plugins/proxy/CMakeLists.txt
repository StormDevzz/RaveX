add_library(ravex_proxy SHARED
    proxy_handler.cpp
    proxy_jni.cpp
)

target_include_directories(ravex_proxy PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_include_directories(ravex_proxy PRIVATE
    ${CMAKE_SOURCE_DIR}/plugins/packet/include
    ${CMAKE_SOURCE_DIR}
)

find_package(JNI REQUIRED)
target_include_directories(ravex_proxy PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(ravex_proxy PRIVATE ${JNI_LIBRARIES})

set_target_properties(ravex_proxy PROPERTIES
    OUTPUT_NAME "ravex_proxy"
    WINDOWS_EXPORT_ALL_SYMBOLS ON
)

if(WIN32)
    set_target_properties(ravex_proxy PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_proxy PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_proxy PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_proxy PROPERTIES SUFFIX ".so")
endif()

set_target_properties(ravex_proxy PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)
