cmake_minimum_required(VERSION 3.16)

add_library(ravex_fileprot SHARED
    fileprot.cpp
    fileprot_jni.cpp
    fileprot_backup.cpp
    fileprot_checksum_db.cpp
    fileprot_config.cpp
    fileprot_hasher.cpp
    fileprot_integrity.cpp
    fileprot_log.cpp
    fileprot_notify.cpp
    fileprot_restore.cpp
    fileprot_scan.cpp
    fileprot_verify.cpp
    fileprot_watcher.cpp
    fileprot_whitelist.cpp
)

target_include_directories(ravex_fileprot PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_fileprot PRIVATE ${JNI_LIBRARIES})

if(NOT WIN32)
    target_link_libraries(ravex_fileprot PRIVATE pthread)
endif()

set_target_properties(ravex_fileprot PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_fileprot PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_fileprot PROPERTIES PREFIX "lib")
endif()
