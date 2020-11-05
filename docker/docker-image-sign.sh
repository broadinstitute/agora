#!/usr/bin/env bash

#
# Functions
#
usage() {
  echo "Usage: ./sign.sh -s <SECRETS_PATH> -p <NOTARY_PROJECT> -z <NOTARY_ZONE> [-n <NOTARY_INSTANCE>] -i <DOCKER_IMAGE>"
  exit 1
}

get_ts_log() {
    date +'%Y-%m-%dT%H:%M:%S%z'
}

vault_cmd() {

    #local _vault_token_file_test
    #local _vault_token_file_dsde
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

    DOCKER_CONTENT_TRUST="" \
        docker run \
            --rm \
            -e VAULT_TOKEN="${_vault_token:?}" \
            -v "$(pwd)/.docker:/root/.docker" \
            broadinstitute/dsde-toolbox vault "${@}"
}

# Parse args
while getopts s:p:z:n:i:k:h flag; do
  case "${flag}" in
    s) export SECRETS_PATH=${OPTARG};;
    p) NOTARY_PROJECT=${OPTARG};;
    z) NOTARY_ZONE=${OPTARG};;
    n) NOTARY_INSTANCE=${OPTARG};;
    i) DOCKER_IMAGE=${OPTARG};;
    k) SERVICE_ACCOUNT_KEY_FILE=${OPTARG};;
    h|*) usage;;
  esac
done

if [ -z "${SECRETS_PATH}" ] \
    || [ -z "${NOTARY_PROJECT}" ] \
    || [ -z "${NOTARY_ZONE}" ] \
    || [ -z "${DOCKER_IMAGE}" ] \
    || [ -z "${SERVICE_ACCOUNT_KEY_FILE}" ]; then
  usage
fi
NOTARY_INSTANCE="${NOTARY_INSTANCE:-notary}"

set -euo pipefail

# fetch and store localhost certificate for the Notary server
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Fetching root-ca.cert for notary..."
TLS_DIR="${HOME}/.docker/tls/localhost:4443"
mkdir -p "${TLS_DIR}"
curl -so "${TLS_DIR}/root-ca.crt" \
  https://raw.githubusercontent.com/theupdateframework/notary/master/fixtures/root-ca.crt

# create IAP tunnel to Notary server
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Creating IAP tunnel..."
export NOTARY_HOST_PORT="localhost:4443"

docker run \
    --rm \
    --net host \
    -v "$(realpath "${SERVICE_ACCOUNT_KEY_FILE}"):/key.json:ro" \
    --name notary-tunnel \
    gcr.io/google.com/cloudsdktool/cloud-sdk:alpine \
        bash -c "gcloud auth activate-service-account --key-file='/key.json' \
            && gcloud compute start-iap-tunnel '${NOTARY_INSTANCE}' 4443 \
                --local-host-port '${NOTARY_HOST_PORT}' \
                --project '${NOTARY_PROJECT}' \
                --zone '${NOTARY_ZONE}'" &
export TUNNEL_PID=$!
trap 'kill "${TUNNEL_PID}"' SIGINT SIGTERM EXIT

# wait for the IAP tunnel to be established
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Waiting for IAP tunnel to be established..."

while ! curl "${NOTARY_HOST_PORT}" &>/dev/null
do
    echo "IAP tunnel is not up; sleeping for 5 seconds before retry."
    sleep 5
done


#tunnel_check_pass_count=0
#tunnel_check_pass_max=3
#tunnel_check_sleep_secs=10
#while ! curl "${NOTARY_HOST_PORT}" &>/dev/null
#do
#    ((tunnel_check_pass_count++))
#    if [[ ${tunnel_check_pass_count:?} -le ${tunnel_check_pass_max:?} ]]; then
#        printf '[%s]: %s Tunnel check attempt: %s. IAP tunnel is not up yet. Will sleep %s seconds, and retry.\n' \
#            "$(get_ts_log)" \
#            "INFO" \
#            "${tunnel_check_pass_count:?}" \
#            "${tunnel_check_sleep_secs:?}"
#        sleep "${tunnel_check_sleep_secs:?}"
#    else
#        printf '[%s] ERROR: IAP_TUNNEL_INIT_FAILED: The IAP tunnel failed to start after %s attempts.\n' \
#            "$(get_ts_log)" \
#            "${tunnel_check_pass_count:?}"
#        exit 1
#    fi
#done

# set up DCT vars
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Setting DOCKER_CONTENT_TRUST env vars..."

export \
  DOCKER_CONTENT_TRUST="1" \
  DOCKER_CONTENT_TRUST_SERVER="https://${NOTARY_HOST_PORT}"

# read/write root passphrase
gen_pass() {
  head -c16 /dev/urandom | md5sum | awk '{ print $1 }'
}


printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Getting or setting DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE..."

if ! DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE=$(vault_cmd read -field pass "${SECRETS_PATH}/root"); then
  DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE=$(gen_pass)

  vault_cmd write "${SECRETS_PATH}/root" pass="${DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE}"
fi

export DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE

# read/write repo passphrase
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Getting or setting DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE..."

if ! DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE=$(vault_cmd read -field pass "${SECRETS_PATH}/repo"); then
  DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE=$(gen_pass)

  vault_cmd write "${SECRETS_PATH}/repo" pass="${DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE}"
fi
export DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE

# load all private keys from Vault into trust store
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Loading private keys from vault into local trust store..."

KEYS_PATH="${HOME}/.docker/trust/private"
mkdir -p "${KEYS_PATH}"
for key in $(vault_cmd list -format json "${SECRETS_PATH}/keys" | jq -r '.[]'); do
    vault_cmd read -field key "${SECRETS_PATH}/keys/${key}" > "${KEYS_PATH}/${key}"
done

# mark signing start time
sign_mark=$(mktemp)

# sign the image
printf '[%s]: %s %s: %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Running docker trust sign for image" \
    "${DOCKER_IMAGE}"

docker trust sign "${DOCKER_IMAGE}"

# store any new private keys back into Vault
printf '[%s]: %s %s\n' \
    "$(get_ts_log)" \
    "INFO" \
    "Storing new private keys in Vault..."

while read -r _file
do
    vault_cmd write "${SECRETS_PATH}/keys/$(basename "${_file}")" key=@"${_file}"
done < <(find "${KEYS_PATH}" -type f -newer "${sign_mark}")

