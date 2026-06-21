cmake_minimum_required(VERSION 3.16)

# RU: Библиотека ravex_lua — поддержка Lua-аддонов в RaveX.
#     Собирается как статическая библиотека. Link в ravex_jni.
# EN: ravex_lua library — Lua addon support for RaveX.
#     Built as a static library. Link into ravex_jni.

# RU: Ищем Lua (LuaJIT предпочтительнее, fallback на Lua 5.1+).
# EN: Find Lua (LuaJIT preferred, fallback to Lua 5.1+).
find_package(LuaJIT QUIET)
if(LuaJIT_FOUND)
    set(LUA_FOUND TRUE)
    set(LUA_INCLUDE_DIR ${LUAJIT_INCLUDE_DIR})
    set(LUA_LIBRARIES ${LUAJIT_LIBRARY})
    message(STATUS "Found LuaJIT: ${LUAJIT_INCLUDE_DIR}")
else()
    find_package(Lua REQUIRED)
    if(LUA_FOUND)
        message(STATUS "Found Lua: ${LUA_INCLUDE_DIR}")
    endif()
endif()

# RU: Если Lua не найден — не крашимся, просто пропускаем.
#     RaveX будет работать без Lua-аддонов.
# EN: If Lua is not found — don't crash, just skip.
#     RaveX will work without Lua addons.
if(NOT LUA_FOUND)
    message(WARNING "Lua not found — Lua addon support will be disabled. "
                    "Install liblua5.1-dev or LuaJIT to enable Lua addons.")
    return()
endif()

add_library(ravex_lua SHARED
    main/LuaTypes.cpp
    main/LuaBridge.cpp
    main/LuaScript.cpp
    main/LuaContext.cpp
    main/LuaAddon.cpp
    main/LuaLoader.cpp
    main/LuaRegistry.cpp
    main/LuaEvent.cpp
    main/LuaModule.cpp
    main/LuaAPI.cpp
)

target_include_directories(ravex_lua PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${LUA_INCLUDE_DIR}
)

target_include_directories(ravex_lua PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/main/cpp/addon/include
)

target_link_libraries(ravex_lua PUBLIC
    ${LUA_LIBRARIES}
)

if(WIN32)
    target_compile_definitions(ravex_lua PRIVATE _WIN32_WINNT=0x0601)
endif()

set_target_properties(ravex_lua PROPERTIES
    CXX_STANDARD 23
    CXX_STANDARD_REQUIRED ON
    POSITION_INDEPENDENT_CODE ON
    OUTPUT_NAME "ravex_lua"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../../../src/main/resources/assets/ravex/natives"
)

if(WIN32)
    set_target_properties(ravex_lua PROPERTIES PREFIX "")
else()
    set_target_properties(ravex_lua PROPERTIES PREFIX "lib")
endif()

if(WIN32)
    set_target_properties(ravex_lua PROPERTIES SUFFIX ".dll")
elseif(UNIX AND NOT APPLE)
    set_target_properties(ravex_lua PROPERTIES SUFFIX ".so")
endif()
