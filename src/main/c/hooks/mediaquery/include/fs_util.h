#pragma once
#include <stddef.h>
#include <stdint.h>

char* find_icon_file(const char* desktop_entry);
uint8_t* read_file(const char* path, size_t* out_len);
