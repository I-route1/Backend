"""Apply GitHub runtime settings to the existing EC2 launcher without logging values."""
import argparse
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import smtplib
import ssl
import subprocess
import sys
import time
import urllib.request

KEYS = ("MAIL_USERNAME", "MAIL_PASSWORD", "KAKAO_ADMIN_KEY")
BEGIN = "# BEGIN I-ROUTE GITHUB RUNTIME SETTINGS"
END = "# END I-ROUTE GITHUB RUNTIME SETTINGS"


def validate_settings(settings):
    missing = [key for key in KEYS if not isinstance(settings.get(key), str) or not settings[key].strip()]
    if missing:
        raise RuntimeError("Missing required GitHub Secrets: " + ", ".join(missing))
    if any("\0" in settings[key] for key in KEYS):
        raise RuntimeError("Runtime setting contains an unsupported NUL character")
    return {key: settings[key] for key in KEYS}


def render_environment(settings):
    return "\n".join("export {}={}".format(key, shlex.quote(settings[key])) for key in KEYS) + "\n"


def patch_launcher(text, env_path):
    block = re.compile(r"(?m)^" + re.escape(BEGIN) + r"\n.*?^" + re.escape(END) + r"\n", re.S)
    text = block.sub("", text)
    launches = list(re.finditer(r"(?m)^[ \t]*(?:(?:nohup|exec)\s+)?(?:java|/[^\s]+/java)\s+", text))
    if len(launches) != 1:
        raise RuntimeError("Expected exactly one supported Java launch in start.sh; nothing was changed")
    position = launches[0].start()
    hook = BEGIN + "\n. " + shlex.quote(str(env_path)) + "\n" + END + "\n"
    return text[:position] + hook + text[position:]


def write_private(path, content):
    flags = os.O_WRONLY | os.O_CREAT | os.O_TRUNC | getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(str(path), flags, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
        os.chmod(path, 0o600)
        stream.write(content)


def verify_mail(settings):
    try:
        with smtplib.SMTP("smtp.gmail.com", 587, timeout=20) as smtp:
            smtp.ehlo()
            smtp.starttls(context=ssl.create_default_context())
            smtp.ehlo()
            smtp.login(settings["MAIL_USERNAME"], settings["MAIL_PASSWORD"])
    except smtplib.SMTPAuthenticationError as error:
        raise RuntimeError("SMTP authentication failed (code {})".format(error.smtp_code)) from None
    except Exception:
        raise RuntimeError("SMTP connection/authentication check failed; current server was not restarted") from None
    print("SMTP authentication verified (no message sent)", flush=True)


def healthy():
    try:
        with urllib.request.urlopen("http://127.0.0.1:8080/health", timeout=3) as response:
            return response.status == 200 and response.read().strip() == b"OK"
    except Exception:
        return False


def runtime_matches(settings):
    for directory in Path("/proc").glob("[0-9]*"):
        try:
            if directory.stat().st_uid != os.getuid():
                continue
            args = (directory / "cmdline").read_bytes().split(b"\0")
            if not args or "java" not in Path(os.fsdecode(args[0])).name:
                continue
            if not any(arg == b"app.jar" or arg.endswith(b"/app.jar") for arg in args):
                continue
            environment = dict(item.split(b"=", 1) for item in (directory / "environ").read_bytes().split(b"\0") if b"=" in item)
            if all(environment.get(key.encode()) == value.encode() for key, value in settings.items()):
                return True
        except (OSError, ValueError):
            continue
    return False


def restart(launcher):
    subprocess.run(["bash", str(launcher)], cwd=str(launcher.parent), stdin=subprocess.DEVNULL,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True, timeout=30)


def wait_until_ready(settings=None):
    for attempt in range(30):
        if healthy() and (settings is None or runtime_matches(settings)):
            return True
        time.sleep(4)
    return False


def deploy(stage):
    stage = stage.resolve()
    if stage.parent.name != ".deploy" or not (stage / "app.jar").is_file():
        raise RuntimeError("Invalid deployment staging directory")
    home = stage.parent.parent
    launcher, app, env_file = home / "start.sh", home / "app.jar", home / ".i_route_runtime.env"
    payload = stage / "runtime-settings.json"
    settings = validate_settings(json.loads(payload.read_text(encoding="utf-8")))
    proposed = patch_launcher(launcher.read_text(encoding="utf-8"), env_file)
    candidate = stage / "start.sh.candidate"
    write_private(candidate, proposed)
    checked = subprocess.run(["bash", "-n", str(candidate)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if checked.returncode:
        raise RuntimeError("Launcher syntax check failed; current server was not changed")
    verify_mail(settings)
    backups = {}
    for name, path in (("launcher", launcher), ("environment", env_file), ("app", app)):
        backup = stage / (name + ".before")
        if path.exists():
            shutil.copyfile(path, backup)
            os.chmod(backup, 0o600)
            backups[path] = backup
        else:
            backups[path] = None
    try:
        write_private(env_file, render_environment(settings))
        os.replace(candidate, launcher)
        shutil.copyfile(stage / "app.jar", home / "app.jar.next")
        os.replace(home / "app.jar.next", app)
        restart(launcher)
        if not wait_until_ready(settings):
            raise RuntimeError("Health/runtime settings verification failed")
    except Exception:
        for path, backup in backups.items():
            if backup is None:
                path.unlink(missing_ok=True)
            else:
                shutil.copyfile(backup, path)
                os.chmod(path, 0o600)
        restart(launcher)
        restored = wait_until_ready()
        raise RuntimeError("Deployment failed; previous version {}".format(
            "restored and healthy" if restored else "restored but health check still failing")) from None
    finally:
        payload.unlink(missing_ok=True)
    print("Health endpoint: 200 OK", flush=True)
    print("Java runtime values match GitHub Secrets: " + ", ".join(KEYS), flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("pack", "deploy"))
    parser.add_argument("path", type=Path)
    args = parser.parse_args()
    os.umask(0o077)
    try:
        if args.operation == "pack":
            settings = validate_settings(os.environ)
            write_private(args.path, json.dumps(settings, ensure_ascii=False))
            print("Required runtime settings are present: " + ", ".join(KEYS))
        else:
            deploy(args.path)
    except Exception as error:
        print(str(error) if isinstance(error, RuntimeError) else "Runtime settings deployment failed", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
