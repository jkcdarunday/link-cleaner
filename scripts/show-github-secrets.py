#!/usr/bin/env python3

import base64
from pathlib import Path
import sys


root = Path(__file__).resolve().parent.parent
properties = dict(
    line.split("=", 1)
    for line in (root / ".signing/keystore.properties").read_text().splitlines()
    if line and not line.startswith("#")
)
values = {
    "RELEASE_KEYSTORE_BASE64": base64.b64encode(
        (root / properties["storeFile"]).read_bytes()
    ).decode("ascii"),
    "RELEASE_STORE_PASSWORD": properties["storePassword"],
    "RELEASE_KEY_ALIAS": properties["keyAlias"],
    "RELEASE_KEY_PASSWORD": properties["keyPassword"],
}

print(
    "These values contain your private signing key and passwords. "
    "Paste them only into GitHub Actions secrets; do not share or commit them.",
    file=sys.stderr,
)
for name, value in values.items():
    print(f"{name}\n{value}\n")
