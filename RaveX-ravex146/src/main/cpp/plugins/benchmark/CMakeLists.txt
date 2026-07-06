cmake_minimum_required(VERSION 3.16)

add_library(ravex_benchmark SHARED
    benchmark.cpp
    benchmark_jni.cpp
    bench_config.cpp
    bench_common.cpp
    bench_log.cpp
    bench_network.cpp
    bench_report.cpp
    bench_result.cpp
    bench_screen.cpp
    bench_suite.cpp
    bench_thread.cpp
    cpu_bench.cpp
    disk_bench.cpp
    memory_bench.cpp
)

target_include_directories(ravex_benchmark PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ravex_benchmark PRIVATE
    ${JNI_LIBRARIES}
)

if(NOT WIN32)
    target_link_libraries(ravex_benchmark PRIVATE pthread)
endif()

set_target_properties(ravex_benchmark PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_benchmark PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_benchmark PROPERTIES PREFIX "lib")
endif()
