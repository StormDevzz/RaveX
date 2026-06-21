cmake_minimum_required(VERSION 3.10)
project(RaveXLauncher CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find packages
find_package(OpenGL REQUIRED)
find_package(SDL2 REQUIRED)

# Include directories
include_directories(
    ${OPENGL_INCLUDE_DIRS}
    ${SDL2_INCLUDE_DIRS}
    imgui
    imgui/backends
    gl3w
)

# Source files
set(SOURCES
    src/main.cpp
    imgui/imgui.cpp
    imgui/imgui_draw.cpp
    imgui/imgui_tables.cpp
    imgui/imgui_widgets.cpp
    imgui/backends/imgui_impl_sdl2.cpp
    imgui/backends/imgui_impl_opengl3.cpp
    gl3w/gl3w.c
)

# Create executable
add_executable(${PROJECT_NAME} ${SOURCES})

# Link libraries
target_link_libraries(${PROJECT_NAME}
    ${OPENGL_LIBRARIES}
    ${SDL2_LIBRARIES}
)

# Platform-specific settings
if(WIN32)
    target_link_libraries(${PROJECT_NAME} setupapi)
endif()

# Copy ImGui and gl3w libraries if needed
# These should be downloaded separately
