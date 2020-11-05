#!/usr/bin/env bash

#
# Functions
#
usage() {
  echo "Usage: ./sign.sh -s <SECRETS_PATH> -p <NOTARY_PROJECT> -z <NOTARY_ZONE> [-n <NOTARY_INSTANCE>] -i <DOCKER_IMAGE>"
  exit 1
}

vault_cmd() {

    local _vault_token_file_test
    local _vault_token_file_dsde
    local _vault_token

    #echo "${@}" >&2

    # TODO: Remove the test token when this works. Happy, Denis?
    #_vault_token_file_test="${HOME:?}/.vault-token"
    #_vault_token_file_dsde="/etc/vault-token-dsde"

    #if [[ -f ${_vault_token_file_test:?} ]]; then
    #    _vault_token="$(cat "${_vault_token_file_test:?}")"
    #elif [[ -f ${_vault_token_file_dsde:?} ]]; then
    #    _vault_token="$(sudo bash -c "cat ${_vault_token_file_dsde:?}")"
    #else
    #    printf 'ERROR NO_VAULT_TOKEN_FILE_FOUND\n'
    #    return 1
    #fi

    _vault_token="$(sudo bash -c "cat /etc/vault-token-dsde")"
    echo "${_vault_token:?}" >&2

    DOCKER_CONTENT_TRUST="" \
        docker run \
            --rm \
            -e VAULT_TOKEN="${_vault_token:?}" \
            -v "$(pwd)/.docker:/root/.docker" \
            broadinstitute/dsde-toolbox vault "${@}"
}

# Parse args
while getopts s:p:z:n:i:h flag; do
  case "${flag}" in
    s) export SECRETS_PATH=${OPTARG};;
    p) NOTARY_PROJECT=${OPTARG};;
    z) NOTARY_ZONE=${OPTARG};;
    n) NOTARY_INSTANCE=${OPTARG};;
    i) DOCKER_IMAGE=${OPTARG};;
    h|*) usage;;
  esac
done

if [ -z "${SECRETS_PATH}" ] || [ -z "${NOTARY_PROJECT}" ] || [ -z "${NOTARY_ZONE}" ] || [ -z "${DOCKER_IMAGE}" ]; then
  usage
fi
NOTARY_INSTANCE="${NOTARY_INSTANCE:-notary}"

set -euo pipefail

# fetch and store localhost certificate for the Notary server
TLS_DIR="${HOME}/.docker/tls/localhost:4443"
mkdir -p "${TLS_DIR}"
curl -so "${TLS_DIR}/root-ca.crt" \
  https://raw.githubusercontent.com/theupdateframework/notary/master/fixtures/root-ca.crt

# set up DCT vars
NOTARY_HOST_PORT="localhost:4443"
export \
  DOCKER_CONTENT_TRUST="1" \
  DOCKER_CONTENT_TRUST_SERVER="https://${NOTARY_HOST_PORT}"

# create IAP tunnel to Notary server
gcloud compute start-iap-tunnel "${NOTARY_INSTANCE}" 4443 \
  --local-host-port "${NOTARY_HOST_PORT}" \
  --project "${NOTARY_PROJECT}" \
  --zone "${NOTARY_ZONE}" &
export TUNNEL_PID=$!
trap 'kill "${TUNNEL_PID}"' SIGINT SIGTERM EXIT

# read/write root passphrase
gen_pass() {
  head -c16 /dev/urandom | md5sum | awk '{ print $1 }'
}

if ! DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE=$(vault_cmd read -field pass "${SECRETS_PATH}/root"); then
  DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE=$(gen_pass)

  vault_cmd write "${SECRETS_PATH}/root" pass="${DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE}"
fi

export DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE

# read/write repo passphrase
if ! DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE=$(vault_cmd read -field pass "${SECRETS_PATH}/repo"); then
  DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE=$(gen_pass)

  vault_cmd write "${SECRETS_PATH}/repo" pass="${DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE}"
fi
export DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE

# load all private keys from Vault into trust store
KEYS_PATH="${HOME}/.docker/trust/private"
mkdir -p "${KEYS_PATH}"
for key in $(vault_cmd list -format json "${SECRETS_PATH}/keys" | jq -r '.[]'); do
    vault_cmd read -field key "${SECRETS_PATH}/keys/${key}" > "${KEYS_PATH}/${key}"
done

# wait for the IAP tunnel to be established
while ! curl "${NOTARY_HOST_PORT}" &>/dev/null; do
  sleep 1
done

# mark signing start time
sign_mark=$(mktemp)

# sign the image
docker trust sign "${DOCKER_IMAGE}"

# store any new private keys back into Vault
while read -r _file
do
    vault_cmd write "${SECRETS_PATH}/keys/$(basename "${_file}")" key=@"${_file}"
done < <(find "${KEYS_PATH}" -type f -newer "${sign_mark}")

