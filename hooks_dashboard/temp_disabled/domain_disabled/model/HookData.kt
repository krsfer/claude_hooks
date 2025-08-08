package com.claudehooks.dashboard.domain.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "hook_data")
data class HookData(
    @PrimaryKey
    val id: String,
    val hook_type: HookType,
    val timestamp: String,
    val session_id: String,
    val sequence: Long,
    @Embedded(prefix = "core_")
    val core: CoreData,
    @Embedded(prefix = "payload_")
    val payload: PayloadData,
    @Embedded(prefix = "context_")
    val context: ContextData
) : Parcelable

@Serializable
@Parcelize
data class CoreData(
    val status: HookStatus,
    val execution_time_ms: Long?
) : Parcelable

@Serializable
@Parcelize
data class PayloadData(
    val prompt: String? = null,
    val tool_name: String? = null,
    val tool_input: String? = null, // JSON string
    val tool_response: String? = null, // JSON string
    val message: String? = null
) : Parcelable

@Serializable
@Parcelize
data class ContextData(
    val platform: Platform,
    val git_branch: String?,
    val git_status: GitStatus?,
    val project_type: ProjectType?
) : Parcelable

@Serializable
enum class HookType {
    session_start,
    user_prompt_submit,
    pre_tool_use,
    post_tool_use,
    notification,
    stop_hook,
    sub_agent_stop_hook,
    pre_compact
}

@Serializable
enum class HookStatus {
    pending,
    success,
    blocked,
    error
}

@Serializable
enum class Platform {
    darwin,
    linux,
    windows,
    unknown
}

@Serializable
enum class GitStatus {
    clean,
    dirty,
    unknown
}

@Serializable
enum class ProjectType {
    react,
    kotlin,
    python,
    android,
    ios,
    web,
    unknown
}