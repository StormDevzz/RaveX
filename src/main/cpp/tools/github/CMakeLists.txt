add_library(ravex_github_tools STATIC
    github_api.cpp
    release_checker.cpp
)

target_include_directories(ravex_github_tools PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
