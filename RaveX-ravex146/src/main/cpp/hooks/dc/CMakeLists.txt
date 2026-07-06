cmake_minimum_required(VERSION 3.14)

add_library(ravex_dc SHARED
    main/discord_rpc.cpp
    main/discord_register.cpp
    main/rpc_lifecycle.cpp
    main/serialization.cpp
    main/dc_jni.cpp
    connect/connection.cpp
    connect/connection_win.cpp
    connect/connection_linux.cpp
    checks/checks.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_dc PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_dc PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_dc")
endif()

target_include_directories(ravex_dc PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/connect
)

set_target_properties(ravex_dc PROPERTIES
    OUTPUT_NAME "ravex_dc"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_dc PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_dc PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_dc PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_dc PROPERTIES SUFFIX ".so")
endif()
