import hashlib
import re
import sys
from pathlib import Path

NATIVES_DIR = Path("src/main/resources/assets/ravex/natives")
MANIFEST_PATH = Path("src/main/java/ravex/utility/nativelib/NativeLoader.java")
BRIDGE_PATH = Path("src/main/java/ravex/loader/NativeBridge.java")
LOADER_FILES = {
    "LOADER_SHA256_LINUX": "libravex_loader.so",
    "LOADER_SHA256_WINDOWS": "ravex_loader.dll",
}


def sha256_hex(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def collect_hashes():
    files = sorted(
        f for f in NATIVES_DIR.iterdir() if f.suffix in (".dll", ".so")
    )
    if not files:
        sys.exit("sync_native_manifest: no native files found in " + str(NATIVES_DIR))
    return {f.name: sha256_hex(f) for f in files}


def sync_manifest(hashes):
    src = MANIFEST_PATH.read_text(encoding="utf-8")
    anchor = "NATIVE_MANIFEST = Map.ofEntries("
    if anchor not in src:
        sys.exit("sync_native_manifest: NATIVE_MANIFEST anchor not found")
    start = src.index(anchor)
    block_start = src.index("\n", start) + 1
    block_end = src.index(");", block_start)
    lines = [
        '        Map.entry("%s", "%s"),' % (name, digest)
        for name, digest in sorted(hashes.items())
    ]
    lines[-1] = lines[-1][:-1]
    out = src[:block_start] + "\n".join(lines) + "\n    " + src[block_end:]
    MANIFEST_PATH.write_text(out, encoding="utf-8")


def sync_bridge(hashes):
    src = BRIDGE_PATH.read_text(encoding="utf-8")
    for const, filename in LOADER_FILES.items():
        if filename not in hashes:
            sys.exit("sync_native_manifest: missing loader native " + filename)
        pattern = re.compile(r'(%s = ")[0-9a-f]+(")' % const)
        if not pattern.search(src):
            sys.exit("sync_native_manifest: constant not found: " + const)
        src = pattern.sub(lambda m: m.group(1) + hashes[filename] + m.group(2), src)
    BRIDGE_PATH.write_text(src, encoding="utf-8")


def main():
    hashes = collect_hashes()
    sync_manifest(hashes)
    sync_bridge(hashes)
    print("sync_native_manifest: synced %d entries" % len(hashes))


if __name__ == "__main__":
    main()
