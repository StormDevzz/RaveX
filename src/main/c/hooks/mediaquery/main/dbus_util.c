#include "dbus_util.h"
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

char* dbus_str(DBusMessageIter* iter) {
    int t = dbus_message_iter_get_arg_type(iter);
    if (t != DBUS_TYPE_STRING && t != DBUS_TYPE_OBJECT_PATH) return NULL;
    const char* val = NULL;
    dbus_message_iter_get_basic(iter, &val);
    return val ? strdup(val) : NULL;
}

char* get_prop(DBusConnection* conn, const char* player, const char* iface, const char* prop) {
    DBusMessage* msg = dbus_message_new_method_call(
        player, "/org/mpris/MediaPlayer2",
        "org.freedesktop.DBus.Properties", "Get");
    if (!msg) return NULL;
    dbus_message_append_args(msg, DBUS_TYPE_STRING, &iface,
                             DBUS_TYPE_STRING, &prop, DBUS_TYPE_INVALID);
    DBusError err;
    dbus_error_init(&err);
    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 500, &err);
    dbus_message_unref(msg);
    if (!reply) { dbus_error_free(&err); return NULL; }
    char* result = NULL;
    DBusMessageIter iter, sub;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) == DBUS_TYPE_VARIANT) {
        dbus_message_iter_recurse(&iter, &sub);
        int t = dbus_message_iter_get_arg_type(&sub);
        if (t == DBUS_TYPE_STRING || t == DBUS_TYPE_OBJECT_PATH) {
            const char* val = NULL;
            dbus_message_iter_get_basic(&sub, &val);
            if (val) result = strdup(val);
        }
    }
    dbus_message_unref(reply);
    return result;
}

char* get_metadata_str(DBusConnection* conn, const char* player, const char* key) {
    DBusMessage* msg = dbus_message_new_method_call(
        player, "/org/mpris/MediaPlayer2",
        "org.freedesktop.DBus.Properties", "Get");
    if (!msg) return NULL;
    const char* iface = "org.mpris.MediaPlayer2.Player";
    const char* prop = "Metadata";
    dbus_message_append_args(msg, DBUS_TYPE_STRING, &iface,
                             DBUS_TYPE_STRING, &prop, DBUS_TYPE_INVALID);
    DBusError err;
    dbus_error_init(&err);
    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 500, &err);
    dbus_message_unref(msg);
    if (!reply) { dbus_error_free(&err); return NULL; }
    char* result = NULL;
    DBusMessageIter iter, sub, dict, entry, variant, val;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_VARIANT) { dbus_message_unref(reply); return NULL; }
    dbus_message_iter_recurse(&iter, &sub);
    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_ARRAY) { dbus_message_unref(reply); return NULL; }
    dbus_message_iter_recurse(&sub, &dict);
    while (dbus_message_iter_get_arg_type(&dict) == DBUS_TYPE_DICT_ENTRY) {
        dbus_message_iter_recurse(&dict, &entry);
        char* entryKey = dbus_str(&entry);
        dbus_message_iter_next(&entry);
        if (dbus_message_iter_get_arg_type(&entry) == DBUS_TYPE_VARIANT) {
            dbus_message_iter_recurse(&entry, &variant);
            if (entryKey && strcmp(entryKey, key) == 0) {
                int vt = dbus_message_iter_get_arg_type(&variant);
                if (vt == DBUS_TYPE_STRING) {
                    result = dbus_str(&variant);
                } else if (vt == DBUS_TYPE_ARRAY) {
                    dbus_message_iter_recurse(&variant, &val);
                    if (dbus_message_iter_get_arg_type(&val) == DBUS_TYPE_STRING) {
                        result = dbus_str(&val);
                    }
                }
                free(entryKey);
                dbus_message_unref(reply);
                return result;
            }
            free(entryKey);
        }
        dbus_message_iter_next(&dict);
    }
    dbus_message_unref(reply);
    return result;
}

int64_t get_prop_int64(DBusConnection* conn, const char* player, const char* iface, const char* prop) {
    DBusMessage* msg = dbus_message_new_method_call(
        player, "/org/mpris/MediaPlayer2",
        "org.freedesktop.DBus.Properties", "Get");
    if (!msg) return -1;
    dbus_message_append_args(msg, DBUS_TYPE_STRING, &iface,
                             DBUS_TYPE_STRING, &prop, DBUS_TYPE_INVALID);
    DBusError err;
    dbus_error_init(&err);
    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 500, &err);
    dbus_message_unref(msg);
    if (!reply) { dbus_error_free(&err); return -1; }
    int64_t result = -1;
    DBusMessageIter iter, sub;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) == DBUS_TYPE_VARIANT) {
        dbus_message_iter_recurse(&iter, &sub);
        if (dbus_message_iter_get_arg_type(&sub) == DBUS_TYPE_INT64) {
            dbus_message_iter_get_basic(&sub, &result);
        }
    }
    dbus_message_unref(reply);
    return result;
}

int64_t get_metadata_int64(DBusConnection* conn, const char* player, const char* key) {
    DBusMessage* msg = dbus_message_new_method_call(
        player, "/org/mpris/MediaPlayer2",
        "org.freedesktop.DBus.Properties", "Get");
    if (!msg) return -1;
    const char* iface = "org.mpris.MediaPlayer2.Player";
    const char* prop = "Metadata";
    dbus_message_append_args(msg, DBUS_TYPE_STRING, &iface,
                             DBUS_TYPE_STRING, &prop, DBUS_TYPE_INVALID);
    DBusError err;
    dbus_error_init(&err);
    DBusMessage* reply = dbus_connection_send_with_reply_and_block(conn, msg, 500, &err);
    dbus_message_unref(msg);
    if (!reply) { dbus_error_free(&err); return -1; }
    int64_t result = -1;
    DBusMessageIter iter, sub, dict, entry, variant;
    dbus_message_iter_init(reply, &iter);
    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_VARIANT) { dbus_message_unref(reply); return -1; }
    dbus_message_iter_recurse(&iter, &sub);
    if (dbus_message_iter_get_arg_type(&sub) != DBUS_TYPE_ARRAY) { dbus_message_unref(reply); return -1; }
    dbus_message_iter_recurse(&sub, &dict);
    while (dbus_message_iter_get_arg_type(&dict) == DBUS_TYPE_DICT_ENTRY) {
        dbus_message_iter_recurse(&dict, &entry);
        char* entryKey = dbus_str(&entry);
        dbus_message_iter_next(&entry);
        if (dbus_message_iter_get_arg_type(&entry) == DBUS_TYPE_VARIANT) {
            dbus_message_iter_recurse(&entry, &variant);
            if (entryKey && strcmp(entryKey, key) == 0) {
                if (dbus_message_iter_get_arg_type(&variant) == DBUS_TYPE_INT64) {
                    dbus_message_iter_get_basic(&variant, &result);
                }
                free(entryKey);
                dbus_message_unref(reply);
                return result;
            }
            free(entryKey);
        }
        dbus_message_iter_next(&dict);
    }
    dbus_message_unref(reply);
    return result;
}
