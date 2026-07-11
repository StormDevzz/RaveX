#pragma once
#include <dbus/dbus.h>
#include <stdint.h>

char* dbus_str(DBusMessageIter* iter);
char* get_prop(DBusConnection* conn, const char* player, const char* iface, const char* prop);
char* get_metadata_str(DBusConnection* conn, const char* player, const char* key);
int64_t get_prop_int64(DBusConnection* conn, const char* player, const char* iface, const char* prop);
int64_t get_metadata_int64(DBusConnection* conn, const char* player, const char* key);
