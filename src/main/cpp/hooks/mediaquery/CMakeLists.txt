cmake_minimum_required(VERSION 3.14)

add_library(ravex_mediaquery SHARED
    mediaquery.cpp
    mediaquery_jni.cpp
)

find_package(JNI REQUIRED)
target_include_directories(ravex_mediaquery PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(ravex_mediaquery PRIVATE ${JNI_LIBRARIES})

target_include_directories(ravex_mediaquery PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_SOURCE_DIR}/common
)

target_link_libraries(ravex_mediaquery PRIVATE ravex_common)

set_property(TARGET ravex_mediaquery PROPERTY CXX_STANDARD 23)
set_property(TARGET ravex_mediaquery PROPERTY CXX_STANDARD_REQUIRED ON)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(ravex_mediaquery PRIVATE /O2 /fp:fast)
    else()
        target_compile_options(ravex_mediaquery PRIVATE -O3 -march=native -ffast-math)
    endif()
endif()

set_target_properties(ravex_mediaquery PROPERTIES
    OUTPUT_NAME "ravex_mediaquery"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_mediaquery PROPERTIES PREFIX "")
    target_link_libraries(ravex_mediaquery PRIVATE winhttp)
else()
    set_target_properties(ravex_mediaquery PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_mediaquery PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_mediaquery PROPERTIES SUFFIX ".so")
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(DBUS REQUIRED dbus-1)
    target_include_directories(ravex_mediaquery PRIVATE ${DBUS_INCLUDE_DIRS})
    target_link_libraries(ravex_mediaquery PRIVATE ${DBUS_LIBRARIES})
    pkg_check_modules(CURL REQUIRED libcurl)
    target_include_directories(ravex_mediaquery PRIVATE ${CURL_INCLUDE_DIRS})
    target_link_libraries(ravex_mediaquery PRIVATE ${CURL_LIBRARIES})
endif()
