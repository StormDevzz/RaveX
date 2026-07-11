#pragma once
#include <stddef.h>
#include <stdint.h>

uint8_t* decode_to_rgba(const uint8_t* data, size_t data_len, int* w, int* h);
uint8_t* resize_rgba(const uint8_t* rgba, int w, int h, int target_w, int target_h);
void free_image(uint8_t* pixels);
