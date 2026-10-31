if(NOT WIN32)
    return()
endif()

add_executable(ravex_native_smoke
    native_smoke.cpp
)

target_compile_features(ravex_native_smoke PRIVATE cxx_std_17)

target_compile_definitions(ravex_native_smoke PRIVATE
    UNICODE
    _UNICODE
    NOMINMAX
    WIN32_LEAN_AND_MEAN
)

set(RAVEX_NATIVE_OUTPUT_DIR
    "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

add_test(
    NAME ravex_native_smoke
    COMMAND ravex_native_smoke "${RAVEX_NATIVE_OUTPUT_DIR}"
)
