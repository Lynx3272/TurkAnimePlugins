import json
import time
from pathlib import Path
from urllib.parse import urlparse

import requests


CONFIG_FILE = Path("config.json")

TIMEOUT = 15
RETRIES = 3

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/140.0 Safari/537.36"
    )
}


def normalize_url(url: str) -> str:
    url = url.strip()

    if not url.startswith(("http://", "https://")):
        url = "https://" + url

    return url.rstrip("/")


def is_valid_url(url: str) -> bool:
    try:
        parsed = urlparse(url)

        return (
            parsed.scheme in ("http", "https")
            and bool(parsed.netloc)
        )

    except Exception:
        return False


def check_domain(url: str) -> dict:

    url = normalize_url(url)

    if not is_valid_url(url):
        return {
            "ok": False,
            "temporary": False,
            "url": url,
            "final_url": None,
            "status": None,
            "error": "Geçersiz URL"
        }

    last_error = None

    for attempt in range(1, RETRIES + 1):

        try:

            response = requests.get(
                url,
                headers=HEADERS,
                timeout=TIMEOUT,
                allow_redirects=True
            )

            final_url = normalize_url(response.url)

            # 2xx ve 3xx cevapları kullanılabilir kabul ediyoruz.
            if 200 <= response.status_code < 400:

                return {
                    "ok": True,
                    "temporary": False,
                    "url": url,
                    "final_url": final_url,
                    "status": response.status_code,
                    "error": None
                }

            # 5xx sunucu hatalarını geçici hata kabul ediyoruz.
            if 500 <= response.status_code < 600:

                last_error = f"HTTP {response.status_code}"

                if attempt < RETRIES:
                    time.sleep(3)
                    continue

                return {
                    "ok": False,
                    "temporary": True,
                    "url": url,
                    "final_url": final_url,
                    "status": response.status_code,
                    "error": last_error
                }

            return {
                "ok": False,
                "temporary": False,
                "url": url,
                "final_url": final_url,
                "status": response.status_code,
                "error": f"HTTP {response.status_code}"
            }

        except requests.RequestException as exc:

            last_error = str(exc)

            if attempt < RETRIES:
                time.sleep(3)
                continue

    return {
        "ok": False,
        "temporary": True,
        "url": url,
        "final_url": None,
        "status": None,
        "error": last_error
    }


def load_config() -> dict:

    if not CONFIG_FILE.exists():
        raise FileNotFoundError(
            f"{CONFIG_FILE} bulunamadı."
        )

    with CONFIG_FILE.open(
        "r",
        encoding="utf-8"
    ) as file:

        return json.load(file)


def save_config(config: dict) -> None:

    with CONFIG_FILE.open(
        "w",
        encoding="utf-8"
    ) as file:

        json.dump(
            config,
            file,
            indent=2,
            ensure_ascii=False
        )

        file.write("\n")


def main() -> None:

    config = load_config()

    changed = False

    for key, current_url in list(config.items()):

        if not isinstance(current_url, str):
            continue

        print(f"\n[{key}]")
        print(f"Kontrol ediliyor: {current_url}")

        result = check_domain(current_url)

        if result["ok"]:

            print(
                f"OK - HTTP {result['status']} "
                f"- {result['final_url']}"
            )

            old_url = normalize_url(current_url)
            new_url = normalize_url(result["final_url"])

            if new_url != old_url:

                print(
                    "Yönlendirme bulundu:"
                    f"\n  {old_url}"
                    f"\n  -> {new_url}"
                )

                config[key] = new_url
                changed = True

        elif result["temporary"]:

            print(
                "GEÇİCİ HATA - "
                f"{result['error']}"
            )

            print(
                "Adres değiştirilmedi."
            )

        else:

            print(
                "BAŞARISIZ - "
                f"{result['error']}"
            )

            print(
                "Adres değiştirilmedi."
            )

    if changed:

        save_config(config)

        print(
            "\nconfig.json güncellendi."
        )

    else:

        print(
            "\nDeğişiklik yok."
        )


if __name__ == "__main__":
    main()