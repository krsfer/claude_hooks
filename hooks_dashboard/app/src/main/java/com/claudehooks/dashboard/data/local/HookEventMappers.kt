package com.claudehooks.dashboard.data.local

import com.claudehooks.dashboard.data.model.HookEvent

fun HookEvent.toEntity(): HookEventEntity {
    return HookEventEntity(
        id = id,
        type = type,
        title = title,
        message = message,
        timestamp = timestamp,
        source = source,
        severity = severity,
        metadata = metadata
    )
}

fun HookEventEntity.toHookEvent(): HookEvent {
    return HookEvent(
        id = id,
        type = type,
        title = title,
        message = message,
        timestamp = timestamp,
        source = source,
        severity = severity,
        metadata = metadata
    )
}

fun List<HookEvent>.toEntities(): List<HookEventEntity> {
    return map { it.toEntity() }
}

fun List<HookEventEntity>.toHookEvents(): List<HookEvent> {
    return map { it.toHookEvent() }
}