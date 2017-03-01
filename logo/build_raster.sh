#!/usr/bin/env bash
#
# Builds a raster logo with given size
#
# Example:
# $ fontinfo.sh -size 64
#
set -eu
set -o pipefail

usage() {
  local prog=$(basename "$0")
  cat <<EOF
usage: ${prog} size

Builds a raster logo with given size
EOF
}

main() {
  if [[ "$@" == "--help" ]]; then
    usage
    exit
  fi

  if [[ "$#" -ne 1 ]]; then
    usage
    exit 1
  fi

  local size=$1
  rsvg-convert logo.svg -o "logo_${size}.png" -w "${size}" -h "${size}"
}

main "$@"
