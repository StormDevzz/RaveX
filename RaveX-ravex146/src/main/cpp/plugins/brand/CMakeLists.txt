add_library(ravex_brand STATIC
    brand.cpp
)

target_include_directories(ravex_brand PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
