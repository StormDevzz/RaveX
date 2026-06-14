#!/usr/bin/env python3
import os, sys, struct

KEY = bytes([0xB0, 0x6F, 0x3A, 0xC7, 0x19, 0x4D, 0xE5, 0x82])

def xor(data):
    return bytes(data[i] ^ KEY[i % len(KEY)] for i in range(len(data)))

def fmt_arr(data):
    return ", ".join(f"0x{b:02x}" for b in data)

token = os.environ.get("RAVEX_TELEGRAM_TOKEN")
chat = os.environ.get("RAVEX_TELEGRAM_CHAT_ID")

if not token or not chat:
    print("ERROR: Set RAVEX_TELEGRAM_TOKEN and RAVEX_TELEGRAM_CHAT_ID env vars", file=sys.stderr)
    sys.exit(1)

etok = xor(token.encode())
echat = xor(chat.encode())

out = f'''#ifndef ENCODED_H
#define ENCODED_H

static const unsigned char ENC_TOKEN[{len(etok)}] = {{{fmt_arr(etok)}}};
static const unsigned char ENC_CHAT[{len(echat)}] = {{{fmt_arr(echat)}}};
static const unsigned char KEY[{len(KEY)}] = {{{fmt_arr(KEY)}}};
static const int KEY_LEN = {len(KEY)};

inline static void decode(const unsigned char* enc, int len, char* out) {{
    for (int i = 0; i < len; i++) {{
        out[i] = enc[i] ^ KEY[i % KEY_LEN];
    }}
    out[len] = 0;
}}

inline static void decode_token(char* out) {{
    decode(ENC_TOKEN, sizeof(ENC_TOKEN), out);
}}

inline static void decode_chat(char* out) {{
    decode(ENC_CHAT, sizeof(ENC_CHAT), out);
}}

#endif
'''

with open(os.path.join(os.path.dirname(__file__), "encoded.h"), "w") as f:
    f.write(out)
print("encoded.h generated")
