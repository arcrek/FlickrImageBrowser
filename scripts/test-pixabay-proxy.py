#!/usr/bin/env python3
"""Probe a temporary localhost proxy; --http3 uses the optional curl_cffi client."""

import argparse
import gzip
import json
import os
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
from threading import Thread
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import ProxyHandler, Request, build_opener, urlopen


def fetch(url, http3=False):
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                      "AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/156.0.0.0 Safari/537.36 Edg/156.0.0.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,"
                  "image/avif,image/webp,image/apng,*/*;q=0.8,"
                  "application/signed-exchange;v=b3;q=0.7",
        "Accept-Encoding": "gzip",
        "Accept-Language": "en-US,en;q=0.9",
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "DNT": "1",
        "Priority": "u=0, i",
        "Referer": "https://pixabay.com/api/docs/",
        "Sec-CH-UA": '"Not:A-Brand";v="8", "Chromium";v="156", "Microsoft Edge";v="156"',
        "Sec-CH-UA-Arch": '"x86"',
        "Sec-CH-UA-Bitness": '"64"',
        "Sec-CH-UA-Full-Version": '"156.0.4314.42"',
        "Sec-CH-UA-Full-Version-List": '"Not:A-Brand";v="8.0.0.0", '
                                       '"Chromium";v="156.0.8078.48", '
                                       '"Microsoft Edge";v="156.0.4314.42"',
        "Sec-CH-UA-Mobile": "?0",
        "Sec-CH-UA-Model": '""',
        "Sec-CH-UA-Platform": '"Windows"',
        "Sec-CH-UA-Platform-Version": '"19.0.0"',
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "same-origin",
        "Sec-Fetch-User": "?1",
        "Upgrade-Insecure-Requests": "1",
    }
    if http3:
        from curl_cffi import CurlHttpVersion, requests
        from curl_cffi.requests.exceptions import RequestException

        try:
            response = requests.get(url, headers=headers, http_version="v3only", timeout=15)
        except RequestException as error:
            return 502, {"Content-Type": "text/plain", "X-Probe-Error": type(error).__name__}, b"HTTP/3 transport failed"
        if response.http_version != CurlHttpVersion.V3:
            return 502, {"Content-Type": "text/plain", "X-Probe-Error": "ProtocolMismatch"}, b"HTTP/3 not negotiated"
        response.headers["X-Probe-Upstream-HTTP"] = "3"
        return response.status_code, response.headers, response.content
    try:
        response = urlopen(Request(url, headers=headers), timeout=15)
    except HTTPError as error:
        response = error
    except (URLError, TimeoutError, OSError) as error:
        return 502, {"Content-Type": "text/plain"}, type(error).__name__.encode()
    with response:
        response.headers["X-Probe-Upstream-HTTP"] = f"{response.version // 10}.{response.version % 10}"
        body = response.read()
        if response.headers.get("Content-Encoding") == "gzip":
            body = gzip.decompress(body)
        return response.code, response.headers, body


def describe(status, headers, body):
    content_type = headers.get("Content-Type", "")
    result = {
        "status": status,
        "content_type": content_type,
        "challenge": headers.get("cf-mitigated") == "challenge",
        "quota_remaining": headers.get("X-RateLimit-Remaining"),
        "bytes": len(body),
        "http_version": headers.get("X-Probe-Upstream-HTTP"),
        "transport_error": headers.get("X-Probe-Error"),
    }
    if "application/json" in content_type:
        try:
            result["hits"] = len(json.loads(body)["hits"])
        except (ValueError, KeyError, TypeError):
            result["invalid_search_response"] = True
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--http3", action="store_true", help="Require HTTP/3 upstream, without fallback")
    args = parser.parse_args()
    if args.http3:
        try:
            import curl_cffi
        except ImportError:
            parser.error("--http3 requires curl_cffi in the active Python environment")

    # Check classification without relying on Pixabay's current availability.
    assert describe(429, {"cf-mitigated": "challenge"}, b"html")["challenge"]
    assert not describe(429, {}, b"API rate limit exceeded")["challenge"]
    assert describe(200, {"Content-Type": "application/json"}, b'{"hits": []}')["hits"] == 0

    key = os.environ.get("PIXABAY_API_KEY", "")
    if not key:
        properties = Path(__file__).resolve().parents[1] / "local.properties"
        for line in properties.read_text().splitlines() if properties.is_file() else ():
            name, separator, value = line.partition("=")
            if separator and name.strip() == "PIXABAY_API_KEY":
                key = value.strip()
                break
    if not key:
        raise SystemExit("Configure PIXABAY_API_KEY in the environment or local.properties.")

    targets = {
        "/search": "https://pixabay.com/api/?" + urlencode({
            "key": key, "q": "yellow flowers", "image_type": "photo", "pretty": "true",
        }),
        "/featured": "https://pixabay.com/api/?" + urlencode({
            "key": key, "editors_choice": "true", "order": "popular",
            "image_type": "photo", "safesearch": "true", "page": 1, "per_page": 30,
        }),
        "/image": "https://pixabay.com/get/g6ffb019d38e05333ff35e639cd155d6d7f00140d3eee3c19009fdd59fa064ba1b8cee6ebb0dc5bf826a140989ccbfbe24ff46d64a74f32e92a26b6a2b2666d87_1280.jpg",
    }
    upstream = {}

    class Proxy(BaseHTTPRequestHandler):
        def log_message(self, *_args):
            pass

        def do_GET(self):
            if self.path not in targets:
                self.send_error(404)
                return
            status, headers, body = fetch(targets[self.path], http3=args.http3)
            upstream[self.path] = (status, body)
            self.send_response(status)
            for name in ("Content-Type", "cf-mitigated", "X-RateLimit-Remaining",
                         "X-Probe-Upstream-HTTP", "X-Probe-Error"):
                if headers.get(name) is not None:
                    self.send_header(name, headers[name])
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

    # Fixed loopback port; an occupied port fails instead of starting another proxy.
    with HTTPServer(("127.0.0.1", 18765), Proxy) as server:
        worker = Thread(target=server.serve_forever)
        worker.start()
        client = build_opener(ProxyHandler({}))
        passed = True
        try:
            for path in ("/search", "/featured", "/image"):
                try:
                    response = client.open("http://127.0.0.1:18765" + path, timeout=25)
                except HTTPError as error:
                    response = error
                with response:
                    body = response.read()
                    # Prove the localhost proxy relayed the upstream status and bytes intact.
                    assert (response.code, body) == upstream[path]
                    result = describe(response.code, response.headers, body)
                    print(json.dumps({"route": path, **result}), flush=True)
                    if path == "/search" and result.get("hits", 0) > 0:
                        targets["/image"] = json.loads(body)["hits"][0]["largeImageURL"]
                    expected = ("image/" in result["content_type"] if path == "/image"
                                else "hits" in result)
                    passed = passed and response.code == 200 and expected
        finally:
            server.shutdown()
            worker.join()
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
