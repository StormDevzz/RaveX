#include "../../../src/main/c/addon/include/c_addon_api.h"
#include <stdio.h>

static ravex_c_addon_api* g_api = NULL;
static int g_tick_count = 0;
static int g_key_presses = 0;

int ravex_c_addon_init(ravex_c_addon_api* api) {
    g_api = api;
    if (g_api && g_api->log_info)
        g_api->log_info("FeatureCAddon loaded");
    return 0;
}

void ravex_c_addon_shutdown(void) {
    if (g_api && g_api->log_info) {
        char buf[128];
        snprintf(buf, sizeof(buf),
            "FeatureCAddon unloaded. Ticks: %d, key presses: %d",
            g_tick_count, g_key_presses);
            g_api->log_info(buf);
        }
    }
}

void ravex_c_addon_on_tick(void) {
    g_tick_count++;
}

void ravex_c_addon_on_key(int key, int action) {
    if (action == 1) {
        g_key_presses++;
        if (g_api && g_api->log_info) {
            char buf[64];
            snprintf(buf, sizeof(buf), "FeatureCAddon: key %d pressed", key);
            g_api->log_info(buf);
        }
    }
}

ravex_c_addon_meta ravex_c_addon_meta_info = {
    .api_version = RAVEX_C_ADDON_API_VERSION,
    .name        = "FeatureCAddon",
    .version     = "1.0.0",
    .description = "C addon with tick and key handlers",
    .author      = "You"
};
