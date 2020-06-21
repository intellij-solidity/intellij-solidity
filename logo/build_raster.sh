#!/usr/bin/env bash
#
# Builds a raster logo with given size
#
set -eu
set -o pipefail

REPO_DIR=$(git rev-parse --show-toplevel)

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
  rsvg-convert "${REPO_DIR}"/src/main/resources/META-INF/pluginIcon.svg -o "${REPO_DIR}/logo/logo_${size}.png" -w "${size}" -h "${size}"
}

main "$@"
