package com.noyize.drive_sync

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.*
import com.google.gson.reflect.TypeToken;
import kotlinx.coroutines.flow.flowOn

class DriveSync<T>(
    private val application: Application,
    googleDriveFolderName: String,
    private val fileName: String
) {

    private val gson = Gson()

    private val driveService = DriveService.get(
        application = application,
        googleSignInAccount = getAccount(),
        folderName = googleDriveFolderName,
    )

    fun getAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(application)
    }


     fun backup(listItem: List<T>) = flow<SyncState<T>>{
            kotlin.runCatching {
                emit(SyncState.Syncing)
                try {
                    val listJson = gson.toJson(listItem)

                    val files: FileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setPageSize(1)
                        .execute()
                    for (file in files.files) {
                        if (file.name == fileName) {
                            driveService.files().delete(file.id)
                        }
                    }
                    val metadata = File().setName(fileName)
                        .setMimeType("application/json")
                        .setParents(Collections.singletonList("appDataFolder"))

                    val contentStream = ByteArrayContent.fromString("application/json", listJson)
                    driveService.files().create(metadata, contentStream)
                        .setFields("id")
                        .execute()

                    emit(SyncState.Success(null))
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(SyncState.Error(e.message ?: "Error restoring notes"))
                }
            }
    }.flowOn(Dispatchers.IO)

     fun restore() = flow {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                emit(SyncState.Syncing)
                try {
                    val files: FileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setPageSize(1)
                        .execute()
                    val backupFile =
                        files.files.singleOrNull { file -> file.name == fileName }

                    backupFile?.let {
                        val outputStream: OutputStream = ByteArrayOutputStream()
                        driveService.files().get(it.id)
                            .executeMediaAndDownloadTo(outputStream)

                        val itemJson = outputStream.toString()
                        val typeToken = object : TypeToken<List<T>>() {}.type
                        val items = gson.fromJson<List<T>>(itemJson, typeToken)

                        emit(SyncState.Success(items))
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(SyncState.Error(e.message ?: "Error restoring notes"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}