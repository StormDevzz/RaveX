add_library(ravex_c_common STATIC
    common.c
)

target_include_directories(ravex_c_common PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

set_property(TARGET ravex_c_common PROPERTY C_STANDARD 23)
set_property(TARGET ravex_c_common PROPERTY C_STANDARD_REQUIRED ON)
