#!/bin/bash

#---------------------------------------------------------
# ヘルパー関数群
#---------------------------------------------------------

# github.com から最終リリースのURLを探す
# Github_getLatestUrl <リポジトリ> <パターン>
function Github_getLatestUrl()
{
    local -r REPO="${1}"
    local -r PAT="${2:-\.jar}"
    local -r URL=$(curl -s "https://api.github.com/repos/${REPO}/releases/latest" | jq -r '.assets[].browser_download_url' | grep "${PAT}" | head -n 1)
    if [ -z "${URL}" ]; then
        echo "最新バージョンが見つかりません：https://github.com/${REPO}"
        exit 1 # abort
    fi
    echo "${URL}"
}

# Download <URL> <出力パス>
function Download()
{
    local -r SRC="${1}"
    local -r DST="${2}"
    # 出力ファイルが存在していたら、タイムスタンプ比較を行い、必要に応じてダウンロードする
    if [ -f "${DST}" ]; then
        OPTS=("-z" "${DST}")
        echo -n "Check & "
    fi
    echo "Download: ${DST}"
    curl -RLs "${SRC}" -o "${DST}" ${OPTS[*]}
    local -i -r fsize="$(stat -c %s ${DST})"
    if [ ${fsize} -lt 64 ]; then
        cat "${DST}"
        echo ""
        rm "${DST}"
        return 1
    fi
    if ! unzip -t "${DST}" > /dev/null; then
        echo "${DST} does not jar"
        rm "${DST}"
        return 1
    fi
    return 0
}

function DownloadFromModrinth()
{
    # Modrinth API のベース URL
    local -r MODRINTH_API="https://api.modrinth.com/v2/project"
    # ダウンロードするプラグインの ID（Modrinth の URL から取得可能）
    PLUGINS=($@)
    for PLUGIN in "${PLUGINS[@]}"; do
        echo "Modrinth から $PLUGIN の最新バージョンを取得しています..."
        # 最新のバージョン情報を取得
        VERSION_ID=$(curl -s "${MODRINTH_API}/${PLUGIN}/version" | jq -r '.[0].id')
        if [ -n "${VERSION_ID}" ]; then
            # ダウンロード URL を取得
            DOWNLOAD_URL=$(curl -s "${MODRINTH_API}/${PLUGIN}/version/${VERSION_ID}" | jq -r '.files[0].url')
            if [ -n "${DOWNLOAD_URL}" ]; then
                DST="${DEST_DIR}/$(basename ${DOWNLOAD_URL})"
                # 出力ファイルが存在していたら、タイムスタンプ比較を行い、必要に応じてダウンロードする
                OPTS=""
                if [ -f "${DST}" ]; then
                    OPTS=("-z" "${DST}")
                    echo -n "Check & "
                fi
                echo "Download: ${DST}"
                curl -sRL "${DOWNLOAD_URL}" -o "${DST}" ${OPTS[*]}
            else
                echo "${PLUGIN} のダウンロード URL を取得できませんでした。"
            fi
        else
            echo "${PLUGIN} の最新バージョン情報を取得できませんでした。"
        fi
    done
}

function DownloadFromSpigot()
{
    # SpigotMC のプラグインページ URL（プラグイン ID を指定）
    SPIGOT_V01_API="https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=${1}"
    #SPIGOT_V2_API="https://api.spiget.org/v2/resources/${1}/versions/latest"

    echo "SpigotMC からプラグインの最新バージョンを取得しています..."
    TITLE=$(curl -sL "${SPIGOT_V01_API}" | jq -r '.title')
    # ダウンロード URL を取得
    #VERSION=$(curl -sL "${SPIGOT_V01_API}" | jq -r '.current_version')
    DOWNLOAD_URL="https://api.spiget.org/v2/resources/${1}/download"

    if [ -n "${DOWNLOAD_URL}" ]; then
        DST="${DEST_DIR}/${TITLE}.jar"
        # 出力ファイルが存在していたら、タイムスタンプ比較を行い、必要に応じてダウンロードする
        OPTS=""
        if [ -f "${DST}" ]; then
            OPTS=("-z" "${DST}")
            echo -n "Check & "
        fi
        echo "Download: ${DST}"
        curl -sRL "${DOWNLOAD_URL}" -o "${DST}" ${OPTS[*]}
    else
        echo "プラグインの URL を取得できませんでした。"
    fi
}

#==========================================================================
# メイン処理
#==========================================================================

# 常にスクリプトのあるディレクトリで実行
cd "$(dirname "$0")"

if [ ! -f .env ]; then
    echo ".env file was not found."
    exit 1
fi

source .env

# ダウンロード先ディレクトリ
DEST_DIR="./plugins"
mkdir -p "${DEST_DIR}"

# 初期化の場合
if [ "${1}" = "clean" ]; then
    rm -rf ./plugins/*.jar
    exit 0
fi

#
# ダウンロード処理（proxy 用）
#
# 最新の ViaVersion のダウンロード URL を取得
URL=$(Github_getLatestUrl ViaVersion/ViaVersion ViaVersion-.*\.jar)
Download "${URL}" "${DEST_DIR}/ViaVersion.jar"
# 最新の ViaBackwards のダウンロード URL を取得
URL=$(Github_getLatestUrl ViaVersion/ViaBackwards ViaBackwards-.*\.jar)
Download "${URL}" "${DEST_DIR}/ViaBackwards.jar"
# 統合版のためのプラグイン
Download "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" "${DEST_DIR}/Geyser-Spigot.jar"
Download "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot" "${DEST_DIR}/floodgate-spigot.jar"
