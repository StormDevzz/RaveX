cmake_minimum_required(VERSION 3.20)
project(packet_sniffer VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(packet_sniffer STATIC
    sniffer.cpp
    capture/pcap_linux.cpp
    capture/proxy.cpp
    protocol/handshake.cpp
    protocol/login.cpp
    protocol/play.cpp
    protocol/status.cpp
    filter/filter.cpp
    output/json_output.cpp
    output/text_output.cpp
    analyze/analyze.cpp
)

target_include_directories(packet_sniffer PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include/../include)
target_include_directories(packet_sniffer PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

find_package(PkgConfig QUIET)
if(PkgConfig_FOUND)
    pkg_check_modules(PCAP libpcap QUIET)
    if(PCAP_FOUND)
        target_include_directories(packet_sniffer PRIVATE ${PCAP_INCLUDE_DIRS})
        target_link_libraries(packet_sniffer PRIVATE ${PCAP_LIBRARIES})
        target_compile_definitions(packet_sniffer PRIVATE HAVE_PCAP=1)
    endif()
endif()

add_executable(packet_sniffer_cli sniffer_cli.cpp)
target_link_libraries(packet_sniffer_cli PRIVATE packet_sniffer)
