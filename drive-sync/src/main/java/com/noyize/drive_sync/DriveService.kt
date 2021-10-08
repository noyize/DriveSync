package com.noyize.drive_sync

import android.app.Application
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.Scopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import java.util.*

internal class DriveService(

) {
    companion object {
        @Volatile
        private var driveService: Drive? = null

        fun get(
            application: Application,
            googleSignInAccount: GoogleSignInAccount?,
            folderName: String
        ): Drive =
            driveService ?: synchronized(this) {

                val credential = GoogleAccountCredential.usingOAuth2(
                    application,
                    Collections.singleton(Scopes.DRIVE_APPFOLDER)
                )
                credential.selectedAccount = googleSignInAccount?.account

                val newInstance = driveService ?: Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName(folderName)
                    .build().also { driveService = it }
                newInstance
            }
    }

}