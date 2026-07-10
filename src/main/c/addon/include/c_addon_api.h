#pragma once
#ifndef RAVEX_C_ADDON_API_H
#define RAVEX_C_ADDON_API_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define RAVEX_C_ADDON_API_VERSION 1

typedef struct {
    void (*log_info)(const char* msg);
    void (*log_warn)(const char* msg);
    void (*log_error)(const char* msg);
    const char* (*get_mc_version)(void);
    bool (*is_key_down)(int key_code);
} ravex_c_addon_api;

typedef struct {
    int         api_version;
    const char* name;
    const char* version;
    const char* description;
    const char* author;
} ravex_c_addon_meta;

typedef int  (*ravex_c_addon_init_fn)(ravex_c_addon_api* api);
typedef void (*ravex_c_addon_shutdown_fn)(void);
typedef void (*ravex_c_addon_on_tick_fn)(void);
typedef void (*ravex_c_addon_on_key_fn)(int key, int action);

#endif
