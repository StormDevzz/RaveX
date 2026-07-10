#include "../../../src/main/c/addon/include/c_addon_api.h"
#include <stdio.h>

static ravex_c_addon_api* g_api = NULL;

int ravex_c_addon_init(ravex_c_addon_api* api) {
    g_api = api;
    if (g_api && g_api->log_info)
        g_api->log_info("MinimalCAddon loaded");
    return 0;
}

void ravex_c_addon_shutdown(void) {
    if (g_api && g_api->log_info)
        g_api->log_info("MinimalCAddon unloaded");
    g_api = NULL;
}

ravex_c_addon_meta ravex_c_addon_meta_info = {
    .api_version = RAVEX_C_ADDON_API_VERSION,
    .name        = "MinimalCAddon",
    .version     = "1.0.0",
    .description = "Minimal C addon",
    .author      = "You"
};
