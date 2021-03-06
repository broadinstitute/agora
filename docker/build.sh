#!/bin/bash

SCRIPT_WORKING_DIR="$(dirname "${0}")"

# The docker-image-sign.sh script will be copied into SCRIPT_WORKING_DIR by
# each Jenkins APPNAME-build job.
DOCKER_IMAGE_SIGNING_SCRIPT="${SCRIPT_WORKING_DIR:?}/docker-image-sign.sh"

HELP_TEXT="$(cat <<EOF
 Build jar and docker images.
   jar : build jar
   -d | --docker : (default: no action) provide either "build" or "push" to
           build or push a docker image.  "push" will also perform build.
   -g | --gcr-registry: If this flag is set, will push to the specified GCR repository.
   -k | --service-account-key-file: (optional) path to a service account key json
           file. If set, the script will call "gcloud auth activate-service-account".
           Otherwise, the script will not authenticate with gcloud.
   -h | --help: print help text.
 Examples:
   Jenkins build job should run with all options, for example,
     ./docker/build.sh jar -d push -g "my-gcr-repo" -k "path-to-my-keyfile"
\t
EOF
)"

# Enable strict evaluation semantics
set -e

# Set default variables
DOCKER_CMD=
BRANCH=${BRANCH:-$(git rev-parse --abbrev-ref HEAD)}  # default to current branch
DOCKERHUB_REGISTRY=${DOCKERHUB_REGISTRY:-broadinstitute/$PROJECT}
GCR_REGISTRY=""
ENV=${ENV:-""}
SERVICE_ACCT_KEY_FILE=""

MAKE_JAR=false
RUN_DOCKER=false
PRINT_HELP=false

if [ -z "$1" ]; then
    echo "No argument supplied!"
    echo "run '${0} -h' to see available arguments."
    exit 1
fi
while [ "$1" != "" ]; do
    case $1 in
        jar)
            MAKE_JAR=true
            ;;
        -d | --docker)
            shift
            echo "docker command = $1"
            DOCKER_CMD=$1
            RUN_DOCKER=true
            ;;
        -g | --gcr-registry)
            shift
            echo "gcr registry = $1"
            GCR_REGISTRY=$1
            ;;
        -k | --service-account-key-file)
            shift
            echo "service-account-key-file = $1"
            SERVICE_ACCT_KEY_FILE=$1
            ;;
        -h | --help)
            PRINT_HELP=true
            ;;
        *)
            echo "Urecognized argument '${1}'."
            echo "run '${0} -h' to see available arguments."
            exit 1
            ;;

    esac
    shift
done

if $PRINT_HELP; then
    echo -e "${HELP_TEXT}"
    exit 0
fi

# Run gcloud auth if a service account key file was specified.
if [[ -n $SERVICE_ACCT_KEY_FILE ]]; then
  TMP_DIR=$(mktemp -d tmp-XXXXXX)
  export CLOUDSDK_CONFIG=$(pwd)/${TMP_DIR}
  gcloud auth activate-service-account --key-file="${SERVICE_ACCT_KEY_FILE}"
fi

# The following function executes the Docker Content Trust signing
# script. This function replaces the docker push -- gcr command in
# the main section of this build script.
docker_content_trust_sign_app_image() {
    local _image_uri

    # Check for param 1: Docker image URI
    if [[ -n ${1} ]]; then
        _image_uri="${1}"
    else
        printf '[%s] %s %s %s: %s\n' \
            "$(date +'%Y-%m-%dT%H:%M:%S%z')" \
            "ERROR" \
            "Missing required param" \
            "1" \
            "image_uri"
        return 1
    fi

    printf '[%s] %s %s: %s\n' \
        "$(date +'%Y-%m-%dT%H:%M:%S%z')" \
        "INFO" \
        "Signing docker image" \
        "${_image_uri:?}"

    # Confirm the docker-image-sign.sh script exists,
    # and execute it with the appropriate params.
    if [[ -f ${DOCKER_IMAGE_SIGNING_SCRIPT:?} ]]; then
        "${DOCKER_IMAGE_SIGNING_SCRIPT:?}" \
            -s "${DOCKER_NOTARY_VAULT_PATH:?}" \
            -p "${DOCKER_NOTARY_GCE_PROJ:?}" \
            -z "${DOCKER_NOTARY_GCE_ZONE:?}" \
            -k "${SERVICE_ACCT_KEY_FILE:?}" \
            -i "${_image_uri:?}"
        return ${?}
    else
        printf '[%s] %s %s: %s\n' \
            "$(date +'%Y-%m-%dT%H:%M:%S%z')" \
            "ERROR" \
            "FILE_NOT_FOUND" \
            "${DOCKER_IMAGE_SIGNING_SCRIPT:?}"
        printf '[%s] %s %s: %s\n' \
            "$(date +'%Y-%m-%dT%H:%M:%S%z')" \
            "ERROR" \
            "PWD_AT_TIME_OF_ERROR" \
            "$(pwd)"
        return 1
   fi
}

function make_jar() {
    echo "building jar..."
    docker run --rm -v $PWD:/working \
            -v jar-cache:/root/.ivy \
            -v jar-cache:/root/.ivy2 \
            broadinstitute/scala-baseimage /working/docker/install.sh /working
}

function docker_cmd()
{
    if [ $DOCKER_CMD = "build" ] || [ $DOCKER_CMD = "push" ]; then
        echo "building docker image..."
        GIT_SHA=$(git rev-parse origin/${BRANCH})
        echo GIT_SHA=$GIT_SHA > env.properties
        IMAGE_TAG="${GIT_SHA:0:12}"
        HASH_TAG="${IMAGE_TAG:?}"

        docker build -t $DOCKERHUB_REGISTRY:${HASH_TAG} .

        echo "scan docker image..."
        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v $HOME/Library/Caches:/root/.cache/ aquasec/trivy --exit-code 1 --severity CRITICAL $DOCKERHUB_REGISTRY:${HASH_TAG}

        if [ $DOCKER_CMD = "push" ]; then
            echo "pushing docker image..."
            docker push $DOCKERHUB_REGISTRY:${HASH_TAG}
            docker tag $DOCKERHUB_REGISTRY:${HASH_TAG} $DOCKERHUB_REGISTRY:${BRANCH}
            docker push $DOCKERHUB_REGISTRY:${BRANCH}

            if [[ -n $GCR_REGISTRY ]]; then
                docker tag $DOCKERHUB_REGISTRY:${HASH_TAG} $GCR_REGISTRY:${HASH_TAG}
                ## Next line replaced with docker_content_trust_sign_app_image.
                #gcloud docker -- push $GCR_REGISTRY:${HASH_TAG}

                # Define the docker image URI using previously-defined
                # vars, and call the signing function to execute the
                # docker-image-sign.sh script.
                IMAGE_URI="${GCR_REGISTRY:?}:${IMAGE_TAG:?}"
                if docker_content_trust_sign_app_image "${IMAGE_URI:?}"; then
                    return 0
                else
                    return 1
                fi
            fi
        fi
    else
        echo "Not a valid docker option!  Choose either build or push (which includes build)"
    fi
}

function cleanup()
{
    echo "cleaning up..."
    if [[ -n $SERVICE_ACCT_KEY_FILE ]]; then
        gcloud auth revoke && echo 'Token revoke succeeded' || echo 'Token revoke failed -- skipping'
      rm -rf ${CLOUDSDK_CONFIG}
    fi
}

if $MAKE_JAR; then
    make_jar
fi

if $RUN_DOCKER; then
    docker_cmd
fi

cleanup
