#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL_PACK_DIR="${ROOT_DIR}/model_pack/src/main/assets"
HF_BASE="https://huggingface.co/litert-community/Gecko-110m-en/resolve/main"

mkdir -p "${MODEL_PACK_DIR}"

echo "Downloading RAG embedder files into Play Asset Delivery pack..."
curl -L "${HF_BASE}/Gecko_256_quant.tflite" -o "${MODEL_PACK_DIR}/Gecko_256_quant.tflite"
curl -L "${HF_BASE}/sentencepiece.model" -o "${MODEL_PACK_DIR}/sentencepiece.model"

echo "Done."
echo "Required files in ${MODEL_PACK_DIR}:"
echo "  - gemma3-1b-it-int4.task   (LLM, ~555MB)"
echo "  - Gecko_256_quant.tflite   (embedder, ~114MB)"
echo "  - sentencepiece.model      (tokenizer)"
echo ""
echo "Build a release AAB and upload to Google Play. Models are NOT bundled in direct APK installs."
