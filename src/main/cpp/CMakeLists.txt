cmake_minimum_required(VERSION 3.16)
project(ravex-native VERSION 1.4.6 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(WIN32)
    if(MSVC)
        set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
        add_compile_definitions(_USE_MATH_DEFINES NOMINMAX)
    else()
        add_link_options(-static -static-libgcc -static-libstdc++)
        add_compile_definitions(_USE_MATH_DEFINES)
    endif()
endif()

find_package(JNI REQUIRED)

add_subdirectory(common)
# add_subdirectory(lua)  # вырублен тк нужен LuaScript.h которого нету
add_subdirectory(math)
# add_subdirectory(hooks/voidesp)  # теперь на джаве
add_subdirectory(hooks/shaders)
add_subdirectory(hooks/norender)
add_subdirectory(hooks/dc)
add_subdirectory(jni)
add_subdirectory(hooks/autocrystal)
add_subdirectory(hooks/trap)
add_subdirectory(hooks/nametags)
# add_subdirectory(hooks/autoapple)  # теперь на джаве
# add_subdirectory(hooks/treecutter) # теперь на джаве
add_subdirectory(hooks/fakepearl)
add_subdirectory(hooks/selftrap)
add_subdirectory(hooks/baseplace)
add_subdirectory(hooks/anchoraura)
add_subdirectory(hooks/breaker)
add_subdirectory(hooks/bowaim)
add_subdirectory(hooks/quiver)
# add_subdirectory(hooks/mediaquery) -- replaced by C version
# add_subdirectory(hooks/safewalk)  # теперь на джаве
# add_subdirectory(hooks/antivoid)  # теперь на джаве
# add_subdirectory(hooks/replenish)  # теперь на джаве
add_subdirectory(hooks/antiquit)
add_subdirectory(hooks/nopacktkick)
add_subdirectory(hooks/phase)
# add_subdirectory(hooks/coordlogger)  # теперь на джаве

# add_subdirectory(hooks/faststairs)  # теперь на джаве
# add_subdirectory(hooks/tunnels)  # теперь нету (обьединен в модуль ESP)
# add_subdirectory(hooks/nofall) # теперь на джаве
add_subdirectory(hooks/nuker)
add_subdirectory(hooks/holefill)
add_subdirectory(hooks/packetmine)
add_subdirectory(hooks/autoclicker)
add_subdirectory(hooks/antibot)
add_subdirectory(hooks/autodrop)
# add_subdirectory(hooks/selffill)  # теперь НЕТУ
add_subdirectory(hooks/burrow)
add_subdirectory(hooks/animations)
add_subdirectory(hooks/autoregear)
add_subdirectory(hooks/antiregear)
# add_subdirectory(hooks/holeesp)  # теперь нету (обьединен в модуль ESP)
add_subdirectory(hooks/tntaura)
# add_subdirectory(hooks/witherroseaura)  # теперь нету
add_subdirectory(hooks/elytraplusplus)
add_subdirectory(hooks/desktopgui) #смехуятина
add_subdirectory(hooks/calculator) #калькУлятор
add_subdirectory(hooks/pearltarget)
add_subdirectory(hooks/antipearl)
add_subdirectory(hooks/bedbomb)
add_subdirectory(hooks/ecfarmer)
add_subdirectory(hooks/shieldfucker)
# add_subdirectory(hooks/noghostblocks)  # теперь на джаве и переименован в GhostBlocks
# add_subdirectory(hooks/portalbuild)  # теперь на джаве и переименован в AutoPortal
add_subdirectory(hooks/chunkexploit)
add_subdirectory(addon)
add_subdirectory(loader)
add_subdirectory(plugins/fastexp)
add_subdirectory(plugins/optimize)
if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/plugins/brand/CMakeLists.txt)
    add_subdirectory(plugins/brand)
endif()
add_subdirectory(plugins/nativesc)
add_subdirectory(plugins/fileprot)
add_subdirectory(plugins/manager)
add_subdirectory(plugins/github)
add_subdirectory(launcher)
