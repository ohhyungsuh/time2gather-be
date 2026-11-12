#!/usr/bin/env python3
"""Require source-matched local checks and classify paid Cloud Build work."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import shlex
import subprocess

INPUTS = ("src", "gradle", "ops/native", "scripts", "Dockerfile", "Dockerfile.jvm",
          "Dockerfile.native", "runtime-lib", "gradle.properties",
          "build.gradle", "settings.gradle", "gradlew", ".dockerignore", ".gcloudignore",
          "public", "package.json", "pnpm-lock.yaml", "astro.config.mjs", "tsconfig.json", "firebase.json")
PURPOSES = ("release", "native", "runtime-validation")


def source_hash(root):
    digest = hashlib.sha256()
    for name in INPUTS:
        target = root / name
        paths = sorted(target.rglob("*")) if target.is_dir() else [target]
        for path in paths:
            if "__pycache__" in path.parts or path.suffix == ".pyc":
                continue
            if path.is_symlink():
                raise ValueError(f"Release inputs must not be symlinks: {path}")
            if path.is_file():
                digest.update(path.relative_to(root).as_posix().encode() + b"\0")
                digest.update(hashlib.sha256(path.read_bytes()).digest())
    return digest.hexdigest()


def validate_config(config, purpose):
    if purpose not in PURPOSES:
        raise ValueError("Unknown build purpose")
    options = config.get("options", {})
    pool = options.get("pool", {}).get("name")
    machine = options.get("machineType", "E2_MEDIUM")
    paid = bool(pool) or machine not in ("E2_MEDIUM", "UNSPECIFIED")
    if paid and purpose == "release":
        raise ValueError("Ordinary releases need the default pool; paid capacity is for native/runtime-validation")
    timeout = config.get("timeout", "600s")
    if not re.fullmatch(r"[1-9][0-9]*s", timeout) or int(timeout[:-1]) > 2400:
        raise ValueError("Build timeout must be between 1 and 2400 seconds")
    if not config.get("steps"):
        raise ValueError("Build config needs steps")
    return {"purpose": purpose, "paidCapacity": paid, "pool": pool, "machine": machine, "timeout": timeout}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="action", required=True)
    preflight = sub.add_parser("preflight")
    preflight.add_argument("--source", type=Path, default=Path.cwd())
    preflight.add_argument("--output", type=Path, required=True)
    preflight.add_argument("--check", action="append", required=True,
                           help="Local command executed without a shell; repeat for each required check")
    submit = sub.add_parser("submit")
    submit.add_argument("--source", type=Path, default=Path.cwd())
    submit.add_argument("--config", type=Path, required=True, help="JSON build config")
    submit.add_argument("--preflight", type=Path, required=True)
    submit.add_argument("--purpose", choices=PURPOSES, required=True)
    submit.add_argument("--project", required=True)
    submit.add_argument("--region", required=True)
    submit.add_argument("--check-only", action="store_true")
    args = parser.parse_args()
    root = args.source.resolve()
    if args.action == "preflight":
        fingerprint = source_hash(root)
        checks = []
        args.output.unlink(missing_ok=True)
        for command in args.check:
            result = subprocess.run(shlex.split(command), cwd=root)
            checks.append({"command": command, "exitCode": result.returncode})
            if result.returncode:
                raise SystemExit(result.returncode)
        if source_hash(root) != fingerprint:
            raise ValueError("Source changed during checks; rerun preflight")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps({"version": 1, "sourceSha256": fingerprint,
            "checkedAt": datetime.now(timezone.utc).isoformat(), "checks": checks}, indent=2) + "\n")
        return
    evidence = json.loads(args.preflight.read_text())
    if (evidence.get("version") != 1 or evidence.get("sourceSha256") != source_hash(root)
            or not evidence.get("checks") or any(c.get("exitCode") != 0 for c in evidence["checks"])):
        raise ValueError("Source needs passing, current preflight checks before a cloud build")
    policy = validate_config(json.loads(args.config.read_text()), args.purpose)
    print(json.dumps(policy), flush=True)
    command = ["gcloud", "builds", "submit", str(root), "--config", str(args.config.resolve()),
               "--project", args.project, "--region", args.region, "--quiet"]
    if args.check_only:
        print(shlex.join(command))
    else:
        raise SystemExit(subprocess.run(command).returncode)


if __name__ == "__main__":
    main()
