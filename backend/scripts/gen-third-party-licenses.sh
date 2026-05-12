#!/usr/bin/env bash
# Regenerates THIRD_PARTY_LICENSES.txt from modules linked by the server build.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
{
  printf '%s\n' 'En Punto — API (Go)' \
    'Third-party libraries (modules reachable from ./..., go list -deps)' ''
  go list -deps -f '{{if .Module}}{{.Module.Path}}@{{.Module.Version}}{{end}}' ./... |
    sort -u |
    grep -v '^$' |
    grep -v '^server@$'
} >THIRD_PARTY_LICENSES.txt
echo "gen-third-party-licenses: wrote ${root}/THIRD_PARTY_LICENSES.txt"
