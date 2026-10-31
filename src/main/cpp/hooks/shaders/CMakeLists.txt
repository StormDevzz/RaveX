add_library(ravex_shaders STATIC
    native/jni_shader_math.cpp
)

target_include_directories(ravex_shaders PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/hand/include
)

target_include_directories(ravex_shaders PRIVATE ${JNI_INCLUDE_DIRS})

# small dll the Shaders module loads on demand
add_library(ravex_shaderfx SHARED
    fx/jni_shader_fx.cpp
)

target_include_directories(ravex_shaderfx PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/hand/include
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_shaderfx PRIVATE ${JNI_LIBRARIES})

set_target_properties(ravex_shaderfx PROPERTIES
    OUTPUT_NAME "ravex_shaderfx"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(MINGW)
    target_link_options(ravex_shaderfx PRIVATE -static-libgcc -static-libstdc++ -static)
endif()

if(WIN32)
    set_target_properties(ravex_shaderfx PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_shaderfx PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_shaderfx PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_shaderfx PROPERTIES SUFFIX ".so")
endif()
