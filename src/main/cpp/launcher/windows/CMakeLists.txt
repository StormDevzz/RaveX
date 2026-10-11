cmake_minimum_required(VERSION 3.16)

if(CMAKE_SOURCE_DIR STREQUAL CMAKE_CURRENT_SOURCE_DIR)
    project(kickx_launcher LANGUAGES CXX)
endif()

set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")

if(WIN32)
    add_compile_definitions(NOMINMAX _USE_MATH_DEFINES)
endif()

string(TIMESTAMP RAVEX_BUILD "%Y%m%d")
configure_file("${CMAKE_CURRENT_SOURCE_DIR}/core/build_info.h.in" "${CMAKE_CURRENT_BINARY_DIR}/build_info.h" @ONLY)

# webp decoding: modrinth serves its icons as webp nowadays, and neither gdi+
# nor wic (its webp codec is missing on many systems - verified wincodec
# err 88982f8b on win10 19044) can decode them. decode-only libwebp is
# fetched at configure time, no runtime dependencies appear.
include(FetchContent)
set(WEBP_BUILD_ANIM_UTILS OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_CWEBP OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_DWEBP OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_GIF2WEBP OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_IMG2WEBP OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_VWEBP OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_WEBPMUX OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_WEBPINFO OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_LIBWEBPMUX OFF CACHE BOOL "" FORCE)
set(WEBP_BUILD_EXTRAS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
FetchContent_Declare(libwebp URL "https://github.com/webmproject/libwebp/archive/refs/tags/v1.4.0.tar.gz")
FetchContent_MakeAvailable(libwebp)

add_executable(kickx_launcher WIN32
    main.cpp
    core/paths.cpp
    core/util.cpp
    core/sha256.cpp
    core/config.cpp
    core/theme.cpp
    core/lang.cpp
    net/http.cpp
    net/json.cpp
    game/mojang.cpp
    game/ravex.cpp
    game/fabric.cpp
    game/launch.cpp
    game/java.cpp
    game/accounts.cpp
    game/serverping.cpp
    game/modupdates.cpp
    game/modpackimport.cpp
    ui/main_window.cpp
    ui/console_window.cpp
    ui/instance_editor.cpp
    ui/account_dialog.cpp
    ui/splash_window.cpp
    ui/settings_dialog.cpp
    ui/color_dialog.cpp
    ui/theme_dialog.cpp
    ui/crash_dialog.cpp
    ui/glow.cpp
    app.rc
)

target_include_directories(kickx_launcher PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}
    ${libwebp_SOURCE_DIR}/src
)

target_compile_features(kickx_launcher PRIVATE cxx_std_23)

if(MSVC)
    target_compile_options(kickx_launcher PRIVATE /utf-8)
elseif(MINGW)
    target_compile_options(kickx_launcher PRIVATE -std=c++23)
    target_link_options(kickx_launcher PRIVATE -static -static-libgcc -static-libstdc++)
    execute_process(COMMAND ${CMAKE_CXX_COMPILER} -dumpspecs OUTPUT_VARIABLE RAVEX_SPECS)
    string(REPLACE "%{!shared:%:if-exists(default-manifest.o%s)}" "" RAVEX_SPECS "${RAVEX_SPECS}")
    file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/ravex.specs" "${RAVEX_SPECS}")
    target_link_options(kickx_launcher PRIVATE "-specs=${CMAKE_CURRENT_BINARY_DIR}/ravex.specs")
endif()

target_link_libraries(kickx_launcher PRIVATE winhttp bcrypt crypt32 shell32 comctl32 comdlg32 user32 gdi32 gdiplus dwmapi uxtheme windowscodecs ws2_32 msimg32 ole32 webpdecoder)
if(MSVC)
    target_link_options(kickx_launcher PRIVATE "/MANIFEST:NO")
endif()
add_custom_command(TARGET kickx_launcher POST_BUILD COMMAND powershell -NoProfile -Command "robocopy '${CMAKE_CURRENT_SOURCE_DIR}/flags' '$<TARGET_FILE_DIR:kickx_launcher>/flags' /E /NFL /NJH /NJS /R:1 /W:0; robocopy '${CMAKE_CURRENT_SOURCE_DIR}/icons' '$<TARGET_FILE_DIR:kickx_launcher>/icons' /E /NFL /NJH /NJS /R:1 /W:0; exit 0")

file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/embedded_launcher.rc" "#include \"resource.h\"\nIDR_EMBEDDED_LAUNCHER RCDATA \"CMakeFiles/kickx_launcher_embedded.exe\"\n")
add_custom_command(
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/kickx_launcher_embedded.exe"
    COMMAND ${CMAKE_COMMAND} -E make_directory "${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles"
    COMMAND ${CMAKE_COMMAND} -E copy "$<TARGET_FILE:kickx_launcher>" "${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/kickx_launcher_embedded.exe"
    DEPENDS kickx_launcher
    VERBATIM
)
add_custom_target(embedded_launcher_copy DEPENDS "${CMAKE_CURRENT_BINARY_DIR}/CMakeFiles/kickx_launcher_embedded.exe")
add_executable(KickXSetup WIN32
    setup/main.cpp
    setup/setup_window.cpp
    ui/glow.cpp
    core/paths.cpp
    core/util.cpp
    core/sha256.cpp
    core/config.cpp
    core/theme.cpp
    core/lang.cpp
    net/http.cpp
    net/json.cpp
    game/mojang.cpp
    game/ravex.cpp
    game/fabric.cpp
    game/launch.cpp
    game/java.cpp
    game/accounts.cpp
    app.rc
    "${CMAKE_CURRENT_BINARY_DIR}/embedded_launcher.rc"
)
add_dependencies(KickXSetup kickx_launcher embedded_launcher_copy)
target_include_directories(KickXSetup PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR})
target_compile_features(KickXSetup PRIVATE cxx_std_23)
if(MSVC)
    target_compile_options(KickXSetup PRIVATE /utf-8)
elseif(MINGW)
    target_compile_options(KickXSetup PRIVATE -std=c++23)
    target_link_options(KickXSetup PRIVATE -static -static-libgcc -static-libstdc++)
    target_link_options(KickXSetup PRIVATE "-specs=${CMAKE_CURRENT_BINARY_DIR}/ravex.specs")
endif()
target_link_libraries(KickXSetup PRIVATE winhttp bcrypt crypt32 shell32 comctl32 comdlg32 user32 gdi32 gdiplus dwmapi uxtheme windowscodecs ws2_32 msimg32 ole32)
if(MSVC)
    target_link_options(KickXSetup PRIVATE "/MANIFEST:NO")
endif()
add_custom_command(TARGET KickXSetup POST_BUILD COMMAND ${CMAKE_COMMAND} -E copy_directory "${CMAKE_CURRENT_SOURCE_DIR}/flags" "$<TARGET_FILE_DIR:KickXSetup>/flags")
add_custom_command(TARGET KickXSetup POST_BUILD COMMAND powershell -NoProfile -Command "robocopy '${CMAKE_CURRENT_SOURCE_DIR}/icons' '$<TARGET_FILE_DIR:KickXSetup>/icons' /E /NFL /NJH /NJS /R:1 /W:0; exit 0")