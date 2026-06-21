cmake_minimum_required(VERSION 3.16)
project(GithubAddon VERSION 1.4.2 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(WIN32)
    if(MSVC)
        add_link_options(/MT)
    else()
        add_link_options(-static -static-libgcc -static-libstdc++)
    endif()
endif()

if(MSVC)
    add_compile_options(/O2 /fp:fast /GL /utf-8)
else()
    add_compile_options(-O3 -march=native -flto -fvisibility=hidden)
    add_link_options(-flto)
endif()

# Path to the RaveX native sources (adjust if needed)
set(RAVEX_SRC ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/main/cpp)

# If building as part of the main project, ravex_github_tools already exists.
# Otherwise, build the github tools as an OBJECT library here.
if(NOT TARGET ravex_github_tools)
    add_library(ravex_github_tools OBJECT
        ${RAVEX_SRC}/plugins/github/src/http_client.cpp
        ${RAVEX_SRC}/plugins/github/src/json.cpp
        ${RAVEX_SRC}/plugins/github/src/release_checker.cpp
        ${RAVEX_SRC}/plugins/github/src/release_manager.cpp
    )
    target_include_directories(ravex_github_tools PUBLIC
        ${RAVEX_SRC}/plugins/github/include
    )
    if(WIN32)
        target_link_libraries(ravex_github_tools PUBLIC winhttp)
    else()
        find_package(OpenSSL REQUIRED)
        target_link_libraries(ravex_github_tools PUBLIC OpenSSL::SSL OpenSSL::Crypto)
    endif()
endif()

add_library(GithubAddon SHARED
    main.cpp
)

target_include_directories(GithubAddon PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/..
    ${RAVEX_SRC}/plugins/github/include
)

target_link_libraries(GithubAddon PRIVATE ravex_github_tools)

set_target_properties(GithubAddon PROPERTIES
    OUTPUT_NAME "GithubAddon"
)

if(WIN32)
    set_target_properties(GithubAddon PROPERTIES PREFIX "" SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(GithubAddon PROPERTIES PREFIX "lib" SUFFIX ".so")
endif()
