add_library(ravex_math STATIC
    wave_math.cpp
)

target_include_directories(ravex_math PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
