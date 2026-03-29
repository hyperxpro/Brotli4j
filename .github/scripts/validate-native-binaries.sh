#!/usr/bin/env bash
#
# Validates that native binaries are present, non-empty, and built for the
# correct architecture. Optionally copies them from a release bundle first.
#
# Architecture detection uses proper binary inspection tools:
#   ELF    -> readelf -h  (Machine, Class, endianness)
#   PE     -> objdump -f  (file format string)
#   Mach-O -> lipo on macOS, raw header read via od on Linux
#
# Usage:
#   validate-native-binaries.sh --bundle <dir>           # copy + validate all 10 platforms
#   validate-native-binaries.sh osx-x86_64 osx-aarch64   # validate specific platforms in-place
#   validate-native-binaries.sh                           # validate all 10 platforms in-place
#
# Exits non-zero if any binary is missing, empty, or has the wrong architecture.
#

set -euo pipefail

BUNDLE=""
if [ "${1:-}" = "--bundle" ]; then
    BUNDLE="${2:?--bundle requires a directory argument}"
    shift 2
fi

FILTER=("${@}")
errors=0

# Each entry: artifact_name:platform:library:expected_arch
NATIVES=(
    "Linux-x86_64-Build-JDK8:linux-x86_64:libbrotli.so:x86_64"
    "Linux-Aarch64-Build-JDK8:linux-aarch64:libbrotli.so:arm64"
    "Linux-ArmV7-Build-JDK8:linux-armv7:libbrotli.so:arm32"
    "Linux-ppc64le-Build-JDK8:linux-ppc64le:libbrotli.so:ppc64le"
    "Linux-riscv64-Build-JDK11:linux-riscv64:libbrotli.so:riscv64"
    "Linux-s390x-Build-JDK8:linux-s390x:libbrotli.so:s390x"
    "Windows-x86_64-Build-JDK8:windows-x86_64:brotli.dll:x86_64"
    "Windows-ARM-Build-JDK8:windows-aarch64:brotli.dll:arm64"
    "MacOS-x86_64-Build-JDK8:osx-x86_64:libbrotli.dylib:x86_64"
    "MacOS-x86_64-Build-JDK8:osx-aarch64:libbrotli.dylib:arm64"
)

should_check() {
    local platform="$1"
    if [ ${#FILTER[@]} -eq 0 ]; then
        return 0
    fi
    for f in "${FILTER[@]}"; do
        [[ "$f" == "$platform" ]] && return 0
    done
    return 1
}

detect_format() {
    file -b "$1"
}

# ── ELF: use readelf to parse Machine, Class, and endianness ──

detect_elf() {
    local f="$1"
    local header
    header=$(readelf -h "$f" 2>/dev/null) || { echo "unknown:unknown"; return; }

    local machine class
    machine=$(echo "$header" | awk -F: '/Machine:/{gsub(/^[[:space:]]+/,"",$2); print $2}')
    class=$(echo "$header" | awk -F: '/Class:/{gsub(/^[[:space:]]+/,"",$2); print $2}')

    local arch
    case "$machine" in
        *X86-64*)    arch="x86_64" ;;
        *AArch64*)   arch="arm64" ;;
        *PowerPC64*)
            # PowerPC64 can be big- or little-endian; we only ship ppc64le
            if echo "$header" | grep -q "little endian"; then
                arch="ppc64le"
            else
                arch="ppc64"
            fi
            ;;
        *RISC-V*)    arch="riscv64" ;;
        *S/390*)     arch="s390x" ;;
        *ARM*)       arch="arm32" ;;
        *)           arch="unknown" ;;
    esac

    local bits
    case "$class" in
        *64*) bits="64" ;;
        *32*) bits="32" ;;
        *)    bits="unknown" ;;
    esac

    echo "${arch}:${bits}"
}

# ── PE: use objdump to parse the file format string ──

detect_pe() {
    local f="$1"
    local format
    format=$(objdump -f "$f" 2>/dev/null | awk '/file format/{print $NF}') || format=""

    case "$format" in
        pei-x86-64)  echo "x86_64:64" ;;
        pei-aarch64) echo "arm64:64" ;;
        pei-i386)    echo "x86:32" ;;
        *)
            # objdump may not support PE Aarch64; read Machine from PE header.
            # DOS header byte 60: 4-byte LE offset to PE signature.
            # PE signature + 4: Machine (2-byte LE).
            #   0x8664 = AMD64, 0xAA64 = ARM64, 0x014C = i386
            local pe_offset machine
            pe_offset=$(od -An -t u4 -j 60 -N 4 "$f" 2>/dev/null | tr -d ' \n') || { echo "unknown:unknown"; return; }
            machine=$(od -An -t x2 -j $((pe_offset + 4)) -N 2 "$f" 2>/dev/null | tr -d ' \n') || { echo "unknown:unknown"; return; }
            case "$machine" in
                8664) echo "x86_64:64" ;;
                aa64) echo "arm64:64" ;;
                014c) echo "x86:32" ;;
                *)    echo "unknown:unknown" ;;
            esac
            ;;
    esac
}

# ── Mach-O: lipo on macOS, raw header read on Linux ──

detect_macho() {
    local f="$1"

    # macOS: use lipo (canonical tool)
    if command -v lipo >/dev/null 2>&1; then
        local archs
        archs=$(lipo -archs "$f" 2>/dev/null) || { echo "unknown:unknown"; return; }
        case "$archs" in
            x86_64) echo "x86_64:64" ;;
            arm64)  echo "arm64:64" ;;
            *" "*)  echo "universal:64" ;;
            *)      echo "unknown:unknown" ;;
        esac
        return
    fi

    # Linux: check for FAT/universal magic before reading thin header.
    # FAT magic is 0xCAFEBABE (BE) or 0xBEBAFECA (LE swapped).
    # od -t x4 reads as a host-endian 32-bit word.
    local magic
    magic=$(od -An -t x4 -N 4 "$f" 2>/dev/null | tr -d ' ') || magic=""
    case "$magic" in
        cafebabe|bebafeca)
            echo "universal:64"
            return
            ;;
    esac

    # Thin Mach-O: read cputype at offset 4.
    # Layout (64-bit LE): magic[4] cputype[4] cpusubtype[4] ...
    #   x86_64 cputype = 0x01000007 -> bytes 07 00 00 01
    #   arm64  cputype = 0x0100000c -> bytes 0c 00 00 01
    local cputype
    cputype=$(od -An -t x1 -j 4 -N 4 "$f" 2>/dev/null | tr -d ' \n') || cputype=""
    case "$cputype" in
        07000001) echo "x86_64:64" ;;
        0c000001) echo "arm64:64" ;;
        *)        echo "unknown:unknown" ;;
    esac
}

# ── Dispatcher: classify format with file(1), then hand off ──

detect_arch() {
    local f="$1"
    local ft
    ft=$(detect_format "$f")

    if [[ "$ft" == *ELF* ]]; then
        detect_elf "$f"
    elif [[ "$ft" == *PE32* ]]; then
        detect_pe "$f"
    elif [[ "$ft" == *Mach-O* ]]; then
        detect_macho "$f"
    else
        echo "unknown:unknown"
    fi
}

# ── Verify binary format matches the platform (ELF/PE/Mach-O) ──

validate_format() {
    local platform="$1"
    local file_out="$2"

    case "$platform" in
        linux-*)   [[ "$file_out" == *ELF* ]]    || return 1 ;;
        windows-*) [[ "$file_out" == *PE32* ]]    || return 1 ;;
        osx-*)     [[ "$file_out" == *Mach-O* ]]  || return 1 ;;
    esac
    return 0
}

# ── Check ELF shared library dependencies (native-arch only) ──
# ldd only works for binaries matching the host architecture.
# On cross-arch ELFs it prints an error (no "not found"), so this
# silently passes — which is fine, it's a best-effort bonus check.

check_deps() {
    local f="$1"

    if file -b "$f" | grep -q ELF; then
        local out
        out=$(ldd "$f" 2>&1 || true)

        # Cross-arch or statically linked — skip gracefully
        if echo "$out" | grep -qiE "not a dynamic executable|cannot execute"; then
            return 0
        fi

        if echo "$out" | grep -q "not found"; then
            return 1
        fi
    fi
    return 0
}

# ── Per-platform check ──

check_native() {
    local artifact="$1" platform="$2" libname="$3" expected_arch="$4"
    local dest="natives/${platform}/target"
    local lib="${dest}/classes/lib/${platform}/${libname}"

    # Copy from bundle if requested
    if [ -n "$BUNDLE" ]; then
        local src="${BUNDLE}/${artifact}/Brotli4j/natives/${platform}/target"
        if [ ! -d "$src" ]; then
            echo "FAIL  ${platform} - artifact directory not found: ${src}"
            return 1
        fi
        rm -rf "$dest"
        cp -r "$src" "$dest"
    fi

    # Binary must exist and be non-empty
    if [ ! -f "$lib" ]; then
        echo "FAIL  ${platform} - ${libname} not found"
        return 1
    fi
    if [ ! -s "$lib" ]; then
        echo "FAIL  ${platform} - ${libname} is empty (zero bytes)"
        return 1
    fi

    # Verify binary format matches the platform
    local file_out
    file_out=$(detect_format "$lib")

    if ! validate_format "$platform" "$file_out"; then
        echo "FAIL  ${platform} - wrong binary format"
        echo "       file: ${file_out}"
        return 1
    fi

    # Detect architecture from binary headers
    local result arch bits
    result=$(detect_arch "$lib")
    IFS=: read -r arch bits <<< "$result"

    if [ "$arch" = "unknown" ] || [ -z "$arch" ]; then
        echo "FAIL  ${platform} - could not detect architecture"
        echo "       file: ${file_out}"
        return 1
    fi

    # Universal Mach-O contains both arches — allow it for macOS targets
    if [[ "$arch" == "universal" && "$platform" == osx-* ]]; then
        bits="64"
    elif [ "$arch" != "$expected_arch" ]; then
        echo "FAIL  ${platform} - expected ${expected_arch}, got ${arch}"
        return 1
    fi

    # armv7 is the only 32-bit target; everything else must be 64-bit
    local expected_bits="64"
    if [ "$expected_arch" = "arm32" ]; then
        expected_bits="32"
    fi

    if [ -z "$bits" ] || [ "$bits" = "unknown" ]; then
        echo "FAIL  ${platform} - could not detect bitness"
        echo "       file: ${file_out}"
        return 1
    fi

    if [ "$bits" != "$expected_bits" ]; then
        echo "FAIL  ${platform} - expected ${expected_bits}-bit, got ${bits}-bit"
        return 1
    fi

    # Best-effort dependency check (only effective for native-arch ELF)
    if ! check_deps "$lib"; then
        echo "FAIL  ${platform} - missing shared library dependencies"
        return 1
    fi

    local size
    size=$(wc -c < "$lib" | tr -d ' ')
    echo "OK    ${platform} (${arch}, ${bits}-bit, ${size} bytes)"
    return 0
}

# ── Main ──

echo "=== Validating native binaries ==="
echo ""

checked=0

for entry in "${NATIVES[@]}"; do
    IFS=: read -r artifact platform libname expected_arch <<< "$entry"

    if ! should_check "$platform"; then
        continue
    fi

    if ! check_native "$artifact" "$platform" "$libname" "$expected_arch"; then
        errors=$((errors + 1))
    fi
    checked=$((checked + 1))
done

echo ""

if [ "$checked" -eq 0 ]; then
    echo "FATAL: no platforms matched the filter: ${FILTER[*]}"
    exit 1
fi

if [ "$errors" -gt 0 ]; then
    echo "FATAL: ${errors}/${checked} checks failed. Aborting release."
    exit 1
fi

echo "All ${checked} native binaries validated successfully."
