#include "mediaquery.h"
#include "fs_util.h"
#include <curl/curl.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>

static size_t write_cb(void* data, size_t size, size_t nmemb, void* user) {
    size_t* ctx = (size_t*)user;
    size_t total = size * nmemb;
    size_t cap = ctx[0], used = ctx[1];
    uint8_t* buf = (uint8_t*)ctx[2];
    if (used + total > cap) {
        size_t newcap = cap ? cap * 2 : 65536;
        if (newcap < used + total) newcap = used + total + 4096;
        uint8_t* nd = realloc(buf, newcap);
        if (!nd) return 0;
        buf = nd;
        ctx[0] = newcap;
        ctx[2] = (size_t)buf;
    }
    memcpy(buf + used, data, total);
    ctx[1] = used + total;
    return total;
}

uint8_t* mediaquery_download_art(const char* url, size_t* out_len) {
    *out_len = 0;
    if (!url || !*url) return NULL;
    if (strncmp(url, "file://", 7) == 0) {
        const char* path = url + 7;
        return read_file(path, out_len);
    }
    CURL* curl = curl_easy_init();
    if (!curl) return NULL;
    size_t ctx[3] = {65536, 0, 0};
    uint8_t* buf = malloc(ctx[0]);
    if (!buf) { curl_easy_cleanup(curl); return NULL; }
    ctx[2] = (size_t)buf;
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_cb);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, ctx);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 5L);
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "RaveX/1.0");
    CURLcode res = curl_easy_perform(curl);
    curl_easy_cleanup(curl);
    if (res != CURLE_OK) { free((void*)ctx[2]); return NULL; }
    *out_len = ctx[1];
    return (uint8_t*)ctx[2];
}