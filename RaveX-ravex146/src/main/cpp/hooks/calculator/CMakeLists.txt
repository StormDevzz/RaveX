cmake_minimum_required(VERSION 3.16)

if(WIN32)
    add_library(ravex_calculator SHARED
        calculator.cpp
        calculator_jni_windows.cpp
    )
else()
    add_library(ravex_calculator SHARED
        calculator.cpp
        calculator_jni.cpp
    )
endif()

target_include_directories(ravex_calculator PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

if(WIN32)
    target_link_libraries(ravex_calculator PRIVATE ${JNI_LIBRARIES} comctl32)
else()
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(GTK3 REQUIRED gtk+-3.0)
    target_include_directories(ravex_calculator PRIVATE ${GTK3_INCLUDE_DIRS})
    target_link_libraries(ravex_calculator PRIVATE
        ${JNI_LIBRARIES}
        ${GTK3_LIBRARIES}
        pthread
    )
endif()

set_target_properties(ravex_calculator PROPERTIES
    OUTPUT_NAME "ravex_calculator"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_calculator PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_calculator PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_calculator PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_calculator PROPERTIES SUFFIX ".so")
endif()
