cmake_minimum_required(VERSION 3.16)

add_library(ravex_addon SHARED
    Addon.cpp
    AddonMeta.cpp
    AddonContext.cpp
    AddonLogger.cpp
    AddonLoader.cpp
    WinAddonLoader.cpp
    LinuxAddonLoader.cpp
    AddonManager.cpp
    AddonJni.cpp
    AddonEvent.cpp
    AddonListener.cpp
    SystemUtils.cpp
    AddonConfig.cpp
    AddonRegistry.cpp
    AddonBypass.cpp
    AddonMath.cpp
    AddonThread.cpp
    AddonMemory.cpp
    AddonVersion.cpp
    AddonError.cpp
)

find_package(JNI REQUIRED)
if(JNI_FOUND)
    target_include_directories(ravex_addon PRIVATE ${JNI_INCLUDE_DIRS})
    target_link_libraries(ravex_addon PRIVATE ${JNI_LIBRARIES})
else()
    message(FATAL_ERROR "JNI not found — cannot build ravex_addon")
endif()

target_include_directories(ravex_addon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_addon PRIVATE ravex_common)

set_property(TARGET ravex_addon PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_addon PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(ravex_addon PRIVATE -O3 -march=native -ffast-math)
endif()

set_target_properties(ravex_addon PROPERTIES
    OUTPUT_NAME "ravex_addon"
    PREFIX "lib"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_addon PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_addon PROPERTIES SUFFIX ".so")
endif()
