#pragma once
#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

typedef struct {
    char* title;
    char* artist;
    char* album;
    char* artUrl;
    char* playerName;
    char* status;
    int64_t position;
    int64_t length;
    bool valid;
} MediaInfo;

MediaInfo* mediaquery_query(void);
void mediaquery_free(MediaInfo* info);
uint8_t* mediaquery_download_art(const char* url, size_t* out_len);
uint8_t* mediaquery_extract_icon(const char* desktop_entry, size_t* out_len);
