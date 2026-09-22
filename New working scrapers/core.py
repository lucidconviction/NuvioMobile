"""Shared utilities for AutoPie IPTV scrapers — ported from TypeScript."""
from __future__ import annotations

import base64
import re
import ssl
import time
import urllib.request
import urllib.error
from dataclasses import dataclass, field
from typing import Optional

UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
CTX = ssl.create_default_context()
CTX.check_hostname = False
CTX.verify_mode = ssl.CERT_NONE


@dataclass
class Stream:
    title: str
    url: str
    group_title: str
    referrer: str = ""
    user_agent: str = ""
    tvg_id: str = ""
    tvg_logo: str = ""


def create_stream(title: str, url: str, group_title: str) -> Stream:
    return Stream(title=title, url=url, group_title=group_title)


def fetch_with_timeout(url: str, timeout_ms: int = 15000, headers: dict | None = None) -> str | None:
    hdrs = {"User-Agent": UA}
    if headers:
        hdrs.update(headers)
    req = urllib.request.Request(url, headers=hdrs)
    try:
        resp = urllib.request.urlopen(req, timeout=timeout_ms / 1000, context=CTX)
        data = resp.read()
        try:
            return data.decode("utf-8")
        except UnicodeDecodeError:
            return data.decode("latin-1")
    except Exception:
        return None


def fetch_json(url: str, timeout_ms: int = 15000, headers: dict | None = None) -> object | None:
    import json
    text = fetch_with_timeout(url, timeout_ms, headers)
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        return None


# ── Embed extraction chain (from embedExtractorChain.ts) ──────────────

def _extract_pattern_a(html: str) -> str | None:
    m = re.search(r'(https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*)', html, re.I)
    if m and 20 < len(m.group(1)) < 2000:
        return m.group(1)
    return None


def _extract_pattern_d(html: str) -> str | None:
    for key in ["source", "file", "src", "url", "hls", "stream", "streamUrl", "hlsUrl"]:
        m = re.search(rf"""['"]{key}['"]\s*:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""", html, re.I)
        if m:
            return m.group(1)
    return None


def _extract_pattern_e(html: str) -> str | None:
    m = re.search(r'function\s+([a-zA-Z0-9_]+)\s*\([a-zA-Z0-9_]\)\s*\{.{0,200}?atob', html, re.S)
    if not m:
        return None
    name = re.escape(m.group(1))
    parts = []
    for call in re.finditer(rf"{name}\s*\(\s*['\"]([A-Za-z0-9+/=_-]+)['\"]\s*\)", html):
        try:
            decoded = base64.b64decode(call.group(1).replace("-", "+").replace("_", "/")).decode("utf-8", errors="ignore")
            parts.append(decoded)
        except Exception:
            pass
    if not parts:
        return None
    assembled = "".join(parts)
    um = re.search(r'(https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*)', assembled, re.I)
    return um.group(1) if um else None


def _extract_pattern_b(html: str) -> str | None:
    for m in re.finditer(r'atob\s*\(\s*["\']([A-Za-z0-9+/=_-]+)["\']\s*\)', html):
        try:
            decoded = base64.b64decode(m.group(1).replace("-", "+").replace("_", "/")).decode("utf-8", errors="ignore")
            if ".m3u8" in decoded or "://" in decoded:
                um = re.search(r'(https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*)', decoded, re.I)
                if um:
                    return um.group(1)
                if re.match(r'^https?://', decoded.strip(), re.I):
                    return decoded.strip()
        except Exception:
            pass

    var_map = {}
    for vm in re.finditer(r'var\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*["\']([A-Za-z0-9+/=_-]{20,})["\']', html):
        var_map[vm.group(1)] = vm.group(2)
    for rm in re.finditer(r'atob\s*\(\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)', html):
        b64 = var_map.get(rm.group(1))
        if b64:
            try:
                decoded = base64.b64decode(b64.replace("-", "+").replace("_", "/")).decode("utf-8", errors="ignore")
                if ".m3u8" in decoded:
                    um = re.search(r'(https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*)', decoded, re.I)
                    if um:
                        return um.group(1)
            except Exception:
                pass
    return None


def _extract_pattern_c(html: str) -> str | None:
    am = re.search(r'var\s+[a-zA-Z_$][a-zA-Z0-9_$]*\s*=\s*\[(\d+(?:,\s*\d+)+)\]', html)
    if not am:
        return None
    nums = [int(n.strip()) for n in am.group(1).split(",")]
    if len(nums) < 10:
        return None
    key_matches = [int(km.group(1)) for km in re.finditer(r'var\s+[a-zA-Z_$][a-zA-Z0-9_$]*\s*=\s*(\d+)', html)]
    if len(key_matches) < 2:
        return None
    for ki in range(len(key_matches) - 1):
        k1, k2 = key_matches[ki], key_matches[ki + 1]
        try:
            decoded = "".join(chr(((n ^ k1 - k2) + 256) % 256) for n in nums)
            if ".m3u8" in decoded:
                um = re.search(r'(https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*)', decoded, re.I)
                if um:
                    return um.group(1)
        except Exception:
            pass
    return None


EXTRACTORS = [
    ("PatternA", _extract_pattern_a),
    ("PatternD", _extract_pattern_d),
    ("PatternE", _extract_pattern_e),
    ("PatternB", _extract_pattern_b),
    ("PatternC", _extract_pattern_c),
]


def extract_from_html(html: str, hint: str = "") -> str | None:
    if not html:
        return None
    ordered = EXTRACTORS
    if "tim" in hint.lower():
        ordered = [EXTRACTORS[4]] + [e for i, e in enumerate(EXTRACTORS) if i != 4]
    for _, fn in ordered:
        try:
            url = fn(html)
            if url and url.startswith("http"):
                return url
        except Exception:
            pass
    return None


def extract_m3u8_from_embed(url: str, hint: str = "") -> str | None:
    html = fetch_with_timeout(url)
    if not html:
        return None
    return extract_from_html(html, hint)


# ── Time helpers ──────────────────────────────────────────────────────

def extract_time_from_text(text: str) -> str | None:
    m = re.search(r'(\d{1,2}:\d{2})\s*(AM|PM|am|pm)?', text)
    if not m:
        return None
    t = m.group(1)
    ampm = (m.group(2) or "").upper()
    return f"{t} {ampm}".strip() if ampm else t


def format_time_pt(timestamp_ms: int) -> str | None:
    import datetime
    try:
        dt = datetime.datetime.fromtimestamp(timestamp_ms / 1000, tz=datetime.timezone(datetime.timedelta(hours=-7)))
        return dt.strftime("%I:%M %p").lstrip("0")
    except Exception:
        return None


def is_within_24hrs_pt(timestamp_ms: int) -> bool:
    import datetime
    now = datetime.datetime.now(tz=datetime.timezone.utc)
    event = datetime.datetime.fromtimestamp(timestamp_ms / 1000, tz=datetime.timezone.utc)
    diff = abs((now - event).total_seconds())
    return diff <= 86400
