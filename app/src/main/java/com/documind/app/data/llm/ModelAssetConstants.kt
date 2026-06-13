package com.documind.app.data.llm

object ModelAssetConstants {
    const val MODEL_PACK_NAME = "model_pack"
    const val LLM_FILE_NAME = "gemma3-1b-it-int4.task"
    const val GECKO_MODEL_FILE_NAME = "Gecko_256_quant.tflite"
    const val TOKENIZER_FILE_NAME = "sentencepiece.model"
    const val MIN_LLM_SIZE_BYTES = 100_000_000L
    const val MIN_GECKO_SIZE_BYTES = 50_000_000L
    const val APPROX_DOWNLOAD_SIZE_MB = 670
}
