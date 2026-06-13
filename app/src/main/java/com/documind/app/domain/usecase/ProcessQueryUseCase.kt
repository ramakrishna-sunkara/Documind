package com.documind.app.domain.usecase

import com.documind.app.data.llm.RagPipelineManager
import com.documind.app.domain.model.DocumentContent
import com.documind.app.util.UserFacingErrors

class ProcessQueryUseCase(
    private val ragPipelineManager: RagPipelineManager
) {

    suspend fun process(
        document: DocumentContent,
        query: String
    ): Result<String> {
        if (!ragPipelineManager.isReady()) {
            return Result.failure(IllegalStateException(UserFacingErrors.ragNotReady()))
        }
        if (!ragPipelineManager.hasIndexedDocument()) {
            return Result.failure(IllegalStateException(UserFacingErrors.documentNotIndexed()))
        }
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a question."))
        }
        return ragPipelineManager.generateResponse(query.trim()).fold(
            onSuccess = { response -> Result.success(response) },
            onFailure = { error ->
                Result.failure(Exception(UserFacingErrors.queryFailed(error)))
            }
        )
    }

    fun isReady(): Boolean = ragPipelineManager.isReady()
}
