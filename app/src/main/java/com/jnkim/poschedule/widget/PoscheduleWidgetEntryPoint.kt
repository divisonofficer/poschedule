package com.jnkim.poschedule.widget

import com.jnkim.poschedule.widget.data.WidgetStateRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing dependencies in widget context.
 *
 * Widgets run in a different context than activities, so we need
 * EntryPoint to access Hilt-provided dependencies.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PoscheduleWidgetEntryPoint {
    fun widgetStateRepository(): WidgetStateRepository
}
