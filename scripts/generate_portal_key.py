#!/usr/bin/env python3
"""
Nuvio Portal Key Generator
Generates NVIO-XXXX-XXXX-XXXX donation keys for the Portal paywall feature.

Usage:
    python generate_portal_key.py [--duration DAYS] [--secret SECRET]

Examples:
    python generate_portal_key.py
    python generate_portal_key.py --duration 90
    python generate_portal_key.py --secret "my-secret" --duration 30
"""

import argparse
import hashlib
import hmac
import json
import os
import sys
import uuid
import base64
import time
from datetime import datetime, timedelta, timezone


DEFAULT_SECRET = "Rdnutz"


def hmac_sha256(data: str, secret: str) -> str:
    return hmac.new(secret.encode(), data.encode(), hashlib.sha256).hexdigest()


def base64_url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode()


def format_key(encoded: str) -> str:
    # Full base64url of "payload.signature". Do NOT truncate or uppercase: the
    # verifier base64url-decodes the whole value and splits on "." to recover the
    # signed payload, so any shortening would make the key unreadable.
    return "NVIO-" + encoded


def generate_key(duration_days: int, secret: str, is_admin: bool = False) -> dict:
    key_id = str(uuid.uuid4())
    # exp is in MILLISECONDS to match the apps (System.currentTimeMillis() /
    # TraktPlatformClock.nowEpochMs()). Generating in seconds made every key read as
    # 1970 and "expired" on device.
    now_ms = int(time.time() * 1000)
    # Admin keys: unlimited portal slots and effectively never expire (100 years).
    if is_admin:
        duration_days = 36500
        expiry_ms = now_ms + (36500 * 24 * 60 * 60 * 1000)
    else:
        expiry_ms = now_ms + (duration_days * 24 * 60 * 60 * 1000)

    payload = json.dumps({
        "id": key_id,
        "exp": expiry_ms,
        "maxDev": 1,
        "isAdmin": is_admin
    }, separators=(',', ':'))
    
    signature = hmac_sha256(payload, secret)
    
    raw = f"{payload}.{signature}"
    encoded = base64_url_encode(raw.encode())
    
    formatted_key = format_key(encoded)
    
    expiry_dt = datetime.fromtimestamp(expiry_ms / 1000, tz=timezone.utc)
    
    return {
        "key": formatted_key,
        "duration": duration_days,
        "expires": expiry_dt.strftime("%Y-%m-%d %H:%M:%S UTC"),
        "key_id": key_id,
        "max_devices": 1,
        "is_admin": is_admin,
    }


def main():
    parser = argparse.ArgumentParser(description="Nuvio Portal Key Generator")
    parser.add_argument("--duration", type=int, default=30, help="Key duration in days (default: 30)")
    parser.add_argument("--secret", type=str, default=None, help="HMAC secret (or set NVIO_KEY_SECRET env var)")
    parser.add_argument("--count", type=int, default=1, help="Number of keys to generate (default: 1)")
    parser.add_argument("--admin", action="store_true", help="Generate an admin key (unlimited portals, never expires)")
    args = parser.parse_args()
    
    secret = args.secret or os.environ.get("NVIO_KEY_SECRET") or DEFAULT_SECRET
    
    if secret == DEFAULT_SECRET:
        print("WARNING: Using default secret. Set --secret or NVIO_KEY_SECRET for production.\n")
    
    print("=== Nuvio Portal Key Generator ===\n")
    
    for i in range(args.count):
        result = generate_key(args.duration, secret, is_admin=args.admin)
        
        print(f"Key:         {result['key']}")
        print(f"Duration:    {result['duration']} days")
        print(f"Expires:     {result['expires']}")
        print(f"Key ID:      {result['key_id']}")
        print(f"Max Devices: {result['max_devices']}")
        print(f"Type:        {'ADMIN' if result['is_admin'] else 'USER'}")
        
        if i < args.count - 1:
            print("\n---\n")
    
    print()


if __name__ == "__main__":
    main()
