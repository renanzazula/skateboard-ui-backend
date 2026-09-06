#!/usr/bin/env bash
#
# Fails if a vendored downstream OpenAPI spec (api/*.yaml) has drifted from the
# upstream service's own api/openapi.yaml. There is no shared contract registry
# (see CLAUDE.md "Generated OpenAPI client"); this is the drift alarm.
#
# Assumes the sibling-repo layout the rest of the docs assume — every
# skateboard-* repo checked out under the same parent directory. Upstream repos
# that aren't present are skipped with a warning (a lone CI checkout can't
# verify them).
#
# Run from the repo root:  ./scripts/check-vendored-specs.sh
#   --warn-only   report drift but exit 0 (there is known pre-existing drift in
#                 the podcast/user/notification specs — hand edits that predate
#                 this script; the app-config spec is clean)
#
# Re-vendor a drifted spec: cp ../<repo>/api/openapi.yaml api/<vendored>.yaml
#   then re-add the leading "# Vendored copy of …" comment block, and check
#   AboutUsController/PodcastController @PreAuthorize vs x-required-permissions.

set -euo pipefail
cd "$(dirname "$0")/.."

warn_only=0
[[ "${1:-}" == "--warn-only" ]] && warn_only=1

# vendored-file : upstream-repo
mappings=(
  "api/openapi.yaml:../skateboard-podcast-be"
  "api/user-openapi.yaml:../skateboard-user-be"
  "api/app-config-openapi.yaml:../skateboard-app-config-be"
  "api/notification-openapi.yaml:../skateboard-notification-be"
)

# Normalise away the bits the vendored copies deliberately customise: the
# leading blank/'#'-comment header, and the `info.title` line (each vendored
# file keeps its own title). Everything else — paths, operations,
# x-required-permissions, schemas — must match the upstream byte for byte.
normalise() {
  awk '
    started || ($0 !~ /^[[:space:]]*#/ && $0 !~ /^[[:space:]]*$/) { started=1 }
    !started { next }
    /^[[:space:]]*title:[[:space:]]/ { print "  title: <normalised>"; next }
    { print }
  ' "$1"
}

drift=0
for entry in "${mappings[@]}"; do
  vendored="${entry%%:*}"
  upstream="${entry#*:}/api/openapi.yaml"

  if [[ ! -f "$upstream" ]]; then
    echo "SKIP  $vendored — upstream not found at $upstream"
    continue
  fi

  if diff -u <(normalise "$upstream") <(normalise "$vendored") > /tmp/spec-drift.diff; then
    echo "OK    $vendored"
  else
    echo "DRIFT $vendored differs from ${entry#*:}/api/openapi.yaml:"
    sed 's/^/    /' /tmp/spec-drift.diff | head -60
    drift=1
  fi
done

if [[ $drift -eq 1 && $warn_only -eq 1 ]]; then
  echo
  echo "(--warn-only: drift found but not failing)"
  exit 0
fi
exit $drift
