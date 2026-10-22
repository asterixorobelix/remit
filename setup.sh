#!/usr/bin/env bash
# Safe, repeatable project initializer. Run --dry-run first to preview changes.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG="$ROOT/app.properties"
# Template identity. The first initialization rewrites all three forms everywhere.
TEMPLATE_PACKAGE="com.example.myproject"
TEMPLATE_TYPE_NAME="MyProject" # classes, themes, iOS product and targets, display name
TEMPLATE_SLUG="myproject"      # Gradle root project, database, JWT issuer
NAME=""
PACKAGE=""
FEATURES="minimal"
DRY_RUN=false
FORCE=false

usage() {
  cat <<'EOF'
Usage: ./setup.sh --name NAME --package com.company.app [options]

NAME may contain letters, digits, '-' and '_'. It becomes APP_NAME, a PascalCase
display/type name (focus-app -> FocusApp), and a lowercase slug (focus_app) used for
the Gradle root project, local database, and JWT issuer.

Options:
  --features LIST  Comma-separated: minimal,auth,offline,backend,maps,billing,firebase
  --dry-run        Print every planned mutation without writing
  --force          Re-run on an initialized tree (moves the package and updates
                   app.properties/xcconfig; class and display names are left as-is)
  --help           Show this help
EOF
}

fail() { printf 'error: %s\n' "$*" >&2; exit 1; }
while (($#)); do
  case "$1" in
    --name|--package|--features)
      if (($# < 2)) || [[ -z "$2" || "$2" == --* ]]; then fail "missing value for $1"; fi
      case "$1" in
        --name) NAME="$2" ;;
        --package) PACKAGE="$2" ;;
        --features) FEATURES="$2" ;;
      esac
      shift 2 ;;
    --dry-run) DRY_RUN=true; shift ;;
    --force) FORCE=true; shift ;;
    --help|-h) usage; exit 0 ;;
    *) fail "unknown argument: $1" ;;
  esac
done

[[ "$NAME" =~ ^[A-Za-z][A-Za-z0-9_-]*$ ]] || fail "--name must start with a letter and contain only letters, digits, _ or -"
[[ "$PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || fail "--package must be a lowercase reverse-domain identifier"
KEYWORDS=" abstract as boolean break byte case catch char class const continue default do double else enum extends false final finally float for fun goto if implements import in instanceof int interface is long native new null object package private protected public return short static super switch synchronized this throw throws transient true try typealias typeof val var void volatile when while "
IFS=. read -r -a segments <<< "$PACKAGE"
for segment in "${segments[@]}"; do
  [[ "$KEYWORDS" != *" $segment "* ]] || fail "--package segment '$segment' is a Kotlin/Java keyword"
done

[[ -n "$FEATURES" && "$FEATURES" != ,* && "$FEATURES" != *, && "$FEATURES" != *,,* ]] || fail "--features must contain a non-empty comma-separated capability list"
IFS=',' read -r -a selected <<< "$FEATURES"
normalized=()
for feature in "${selected[@]}"; do
  case ",$feature," in
    ,minimal,|,auth,|,offline,|,backend,|,maps,|,billing,|,firebase,) ;;
    *) fail "unsupported capability '$feature'" ;;
  esac
  [[ "$feature" == minimal ]] || normalized+=("$feature")
done
((${#normalized[@]})) || normalized=(minimal)
FEATURES="$(IFS=,; echo "${normalized[*]}")"

pascal_case() {
  local part out=""
  IFS='_-' read -r -a parts <<< "$1"
  for part in "${parts[@]}"; do
    [[ -n "$part" ]] || continue
    out+="$(printf '%s' "${part:0:1}" | tr '[:lower:]' '[:upper:]')${part:1}"
  done
  printf '%s' "$out"
}
TYPE_NAME="$(pascal_case "$NAME")"
SLUG="$(printf '%s' "$NAME" | tr '[:upper:]-' '[:lower:]_')"

CURRENT_PACKAGE="$(awk -F= '$1=="PACKAGE_NAME" {print $2}' "$CONFIG")"
INITIAL=false
[[ "$CURRENT_PACKAGE" == "$TEMPLATE_PACKAGE" ]] && INITIAL=true
if ! $INITIAL && ! $FORCE; then
  fail "already initialized as $CURRENT_PACKAGE; pass --force only if you intend another exact rename"
fi
if $INITIAL && printf '%s' "$PACKAGE" | grep -qi "$TEMPLATE_SLUG"; then
  fail "--package must not contain '$TEMPLATE_SLUG' (the template name that setup replaces)"
fi

OLD_PATH="$(printf '%s' "$CURRENT_PACKAGE" | tr . /)"
NEW_PATH="$(printf '%s' "$PACKAGE" | tr . /)"

# Ordered search/replace pairs: the package forms first, since they contain the slug.
FROM=("$CURRENT_PACKAGE" "$OLD_PATH")
TO=("$PACKAGE" "$NEW_PATH")
if $INITIAL; then
  FROM+=("$TEMPLATE_TYPE_NAME" "$TEMPLATE_SLUG")
  TO+=("$TYPE_NAME" "$SLUG")
fi

# Generated, vendored, and history directories are never rewritten.
PRUNE=(\( -name .git -o -name build -o -name .gradle -o -name .kotlin -o -name .idea -o -name .planning -o -name DerivedData -o -name node_modules \) -prune -o)

# Package directories in every source set, including SQLDelight roots. Validate moves
# before rewriting files so a conflicting directory cannot leave a partial rename.
PACKAGE_DIRS=()
if [[ "$OLD_PATH" != "$NEW_PATH" ]]; then
  while IFS= read -r dir; do
    base="${dir%"/$OLD_PATH"}"
    [[ -e "$base/$NEW_PATH" ]] && fail "package destination already exists: $base/$NEW_PATH"
    PACKAGE_DIRS+=("$dir")
  done < <(find "$ROOT" "${PRUNE[@]}" -type d -path "*/$OLD_PATH" -print)
fi

grep_args=()
for pattern in "${FROM[@]}"; do grep_args+=(-e "$pattern"); done
INIT_FILES=()
while IFS= read -r file; do
  case "$file" in
    "$ROOT/setup.sh"|"$ROOT/scripts/test-initializer.sh") ;; # keep template constants
    *) INIT_FILES+=("$file") ;;
  esac
done < <(find "$ROOT" "${PRUNE[@]}" -type f -exec grep -IlF "${grep_args[@]}" {} +)

# Maintainer-only history of the template itself; generated apps start without it.
MAINTAINER_PATHS=()
if $INITIAL; then
  [[ -e "$ROOT/.planning" ]] && MAINTAINER_PATHS+=("$ROOT/.planning")
fi

NAMED_PATHS=()
if $INITIAL; then
  # Depth-first so children are renamed before their parent directories.
  while IFS= read -r path; do NAMED_PATHS+=("$path"); done \
    < <(find "$ROOT" "${PRUNE[@]}" -depth -name "*$TEMPLATE_TYPE_NAME*" -print)
fi

printf 'Initialize %s (%s), type name %s, slug %s, capabilities: %s\n' "$NAME" "$PACKAGE" "$TYPE_NAME" "$SLUG" "$FEATURES"
$INITIAL || printf 'Re-initializing %s: class and display names keep their current values.\n' "$CURRENT_PACKAGE"

if $DRY_RUN; then
  for i in "${!FROM[@]}"; do printf '+ replace %q -> %q\n' "${FROM[$i]}" "${TO[$i]}"; done
  printf '+ rewrite %s files\n' "${#INIT_FILES[@]}"
  for dir in "${PACKAGE_DIRS[@]:-}"; do [[ -z "$dir" ]] || printf '+ move package %q -> %q\n' "$dir" "${dir%"/$OLD_PATH"}/$NEW_PATH"; done
  for path in "${NAMED_PATHS[@]:-}"; do [[ -z "$path" ]] || printf '+ rename %q\n' "$path"; done
  for path in "${MAINTAINER_PATHS[@]:-}"; do [[ -z "$path" ]] || printf '+ remove %q\n' "$path"; done
else
  if ((${#INIT_FILES[@]})); then
    SETUP_PAIRS="$(for i in "${!FROM[@]}"; do printf '%s\t%s\n' "${FROM[$i]}" "${TO[$i]}"; done)" perl -pi -e '
      BEGIN { @pairs = map { [split /\t/, $_, 2] } split /\n/, $ENV{SETUP_PAIRS} }
      for my $pair (@pairs) { s/\Q$pair->[0]\E/$pair->[1]/g }
    ' "${INIT_FILES[@]}"
  fi

  # Rename name-bearing paths before package moves change their parent directories.
  for path in "${NAMED_PATHS[@]:-}"; do
    [[ -n "$path" ]] || continue
    name="$(basename "$path")"
    mv "$path" "$(dirname "$path")/${name//$TEMPLATE_TYPE_NAME/$TYPE_NAME}"
  done

  for from in "${PACKAGE_DIRS[@]:-}"; do
    [[ -n "$from" ]] || continue
    base="${from%"/$OLD_PATH"}"
    to="$base/$NEW_PATH"
    # Stage outside the old package to support a new package nested beneath it.
    stage="$(mktemp -d "$base/.setup-package.XXXXXX")"
    mv "$from" "$stage/package"
    mkdir -p "$(dirname "$to")"
    mv "$stage/package" "$to"
    rmdir "$stage"
    parent="$(dirname "$from")"
    while [[ "$parent" != "$base" ]] && rmdir "$parent" 2>/dev/null; do parent="$(dirname "$parent")"; done
  done

  for path in "${MAINTAINER_PATHS[@]:-}"; do [[ -z "$path" ]] || rm -rf "$path"; done

  perl -pi -e "s/^APP_NAME=.*/APP_NAME=$NAME/; s/^PACKAGE_NAME=.*/PACKAGE_NAME=$PACKAGE/; s/^FEATURES=.*/FEATURES=$FEATURES/; s/^IOS_BILLING_PRODUCT=.*/IOS_BILLING_PRODUCT=$PACKAGE.premium.monthly/" "$CONFIG"
  if $INITIAL; then
    perl -pi -e "s/^PRODUCT_NAME=.*/PRODUCT_NAME=$TYPE_NAME/" "$ROOT/mobile/iosApp/Configuration/Config.xcconfig"
  fi
  "$ROOT/scripts/sync-app-config.sh" >/dev/null
  (cd "$ROOT" && ./setup-hooks.sh)
fi

printf '%s complete. Run make doctor, then make check.\n' "$($DRY_RUN && echo Preview || echo Initialization)"
printf 'App-specific work: run make app-checklist (APP_CHECKLIST.md), including adding Share app before launch.\n'
