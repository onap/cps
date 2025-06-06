#!/bin/bash
#
# Copyright 2025 OpenInfra Foundation Europe. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Exit on error, undefined variable, or pipefail
set -euo pipefail

# List of Docker images to check - passed via environment or hardcoded
IMAGES_TO_CHECK=("${IMAGES_TO_CHECK[@]:-}")

# Directory to store digest files
DIGEST_DIR="${WORKSPACE:-.}/image/digest"
mkdir -p "$DIGEST_DIR"

# Function to sanitize image names for filenames
sanitize_image_name() {
  local image="$1"
  echo "$image" | sed 's/[:\/]/_/g'
}

# Function to retrieve the digest of a local image
get_local_digest() {
  local image="$1"
  docker inspect --format='{{index .RepoDigests 0}}' "$image" 2>/dev/null || echo "none"
}

# Function to pull the latest image and retrieve its digest
pull_image_and_get_digest() {
  local image="$1"
  local pull_output

  # Capture pull output and error
  if ! pull_output=$(docker pull "$image" 2>&1); then
    echo "⛔ Failed to pull image: $image"
    if echo "$pull_output" | grep -q "toomanyrequests:"; then
      echo "   ❗ Detected Docker Hub rate limit (toomanyrequests:)"
    fi
    echo "   ⚠️  Possible reasons:"
    echo "     - Network issues"
    echo "     - Image does not exist"
    echo "     - Docker Hub rate limit exceeded"
    echo "   🔄 Attempting to retrieve local digest (if available)..."
    get_local_digest "$image"
    return 1
  fi

  get_local_digest "$image"
}

# Main image validation logic
check_images() {
  echo
  echo "🔍 Starting Docker image verification..."
  echo "⏱️  Started at   : $(date -u)"

  for IMAGE in "${IMAGES_TO_CHECK[@]}"; do
    echo -e "\n🧪 Verifying Docker image: $IMAGE"
    echo "   🕒 Timestamp: $(date -u)"

    local SANITIZED_IMAGE_NAME
    SANITIZED_IMAGE_NAME=$(sanitize_image_name "$IMAGE")
    local DIGEST_FILE="$DIGEST_DIR/${SANITIZED_IMAGE_NAME}.digest"

    local PREVIOUS_DIGEST="none"
    if [[ -f "$DIGEST_FILE" ]]; then
      PREVIOUS_DIGEST=$(<"$DIGEST_FILE")
    fi

    local NEW_DIGEST
    if ! NEW_DIGEST=$(pull_image_and_get_digest "$IMAGE"); then
      echo "   ⚠️  Using local image (if available)."
      continue
    fi

    if [[ "$PREVIOUS_DIGEST" != "$NEW_DIGEST" ]]; then
      echo "⚠️  Digest changed for $IMAGE"
      echo "   🔖 Previous digest: $PREVIOUS_DIGEST"
      echo "   🆕 New digest     : $NEW_DIGEST"
      echo "$NEW_DIGEST" > "$DIGEST_FILE"
      log_image_info "$IMAGE"
    else
      echo "✅ Image content is unchanged. Digest remains:"
      echo "   🔖 $NEW_DIGEST"
    fi
  done

  echo -e "\n✅ Docker image verification completed for all specified images."
  echo "🕒 Finished at: $(date -u)"
}

# Function: Display local metadata for an image
# Function: Display detailed metadata for a local Docker image with styled output
log_image_info() {
  local IMAGE_NAME="$1"
  echo "🖼️  Docker Image Metadata for: $IMAGE_NAME"

  local IMAGE_ID CREATED SIZE DIGEST
  IMAGE_ID=$(docker inspect --format='{{.Id}}' "$IMAGE_NAME" 2>/dev/null || echo "❓ unknown")
  CREATED=$(docker inspect --format='{{.Created}}' "$IMAGE_NAME" 2>/dev/null || echo "❓ unknown")
  SIZE=$(docker inspect --format='{{.Size}}' "$IMAGE_NAME" 2>/dev/null || echo "❓ unknown")
  DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "$IMAGE_NAME" 2>/dev/null || echo "❓ unknown")

  echo "  🆔 Image ID     : $IMAGE_ID"
  echo "  📅 Created      : $CREATED"

  if [[ "$SIZE" != "❓ unknown" && "$SIZE" =~ ^[0-9]+$ ]]; then
    # Using awk to avoid bc dependency
    SIZE_MB=$(awk "BEGIN { printf \"%.2f\", $SIZE/1024/1024 }")
    echo "  📦 Size         : ${SIZE_MB} MB"
  else
    echo "  📦 Size         : ❓ unknown"
  fi
  echo "  🔖 Digest       : $DIGEST"
}