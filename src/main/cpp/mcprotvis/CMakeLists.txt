cmake_minimum_required(VERSION 3.20)
project(mcprotvis VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)

find_package(Qt6 REQUIRED COMPONENTS Widgets)

add_executable(mcprotvis
    misc/main.cpp
    misc/generate_html.cpp
    include/main_window.hpp
    display/main_window.cpp
    display/pcap_loader.cpp
    display/packet_info.cpp
    display/hex_format.cpp
    reader/reader_pcap.cpp
    reader/reader_ethernet.cpp
    reader/reader_session.cpp
    packet/packet_parse.cpp
    packet/packet_names.cpp
    packet/packet_lookup.cpp
    display/print_packet.cpp
    display/print_summary.cpp
    display/print_hex.cpp
)

target_include_directories(mcprotvis PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_link_libraries(mcprotvis PRIVATE Qt6::Widgets)
