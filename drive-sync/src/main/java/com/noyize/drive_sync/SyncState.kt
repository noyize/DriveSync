package com.noyize.drive_sync

sealed class SyncState<out T>(){
    object Syncing: SyncState<Nothing>()
    data class Success<out T>(val value: T?) : SyncState<T>()
    data class Error(val message: String): SyncState<Nothing>()
}