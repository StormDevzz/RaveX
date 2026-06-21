add_library(ravex_github_tools SHARED
    src/http_client.cpp
    src/json.cpp
    src/release_checker.cpp
    src/release_manager.cpp
)

target_include_directories(ravex_github_tools PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

if(WIN32)
    target_link_libraries(ravex_github_tools PRIVATE winhttp)
else()
    find_package(OpenSSL REQUIRED)
    target_link_libraries(ravex_github_tools PRIVATE OpenSSL::SSL OpenSSL::Crypto)
endif()

set_target_properties(ravex_github_tools PROPERTIES
    OUTPUT_NAME "ravex_github_tools"
)

if(WIN32)
    set_target_properties(ravex_github_tools PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_github_tools PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_github_tools PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_github_tools PROPERTIES SUFFIX ".so")
endif()
