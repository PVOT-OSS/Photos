// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.viewer

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.prauga.photos.R
import org.prauga.photos.data.MediaItem

/** Share, favourite and delete, each through the system's own confirmation where it has one. */
class MediaActions(
    private val context: Context,
    private val launch: (IntentSenderRequest) -> Unit,
) {
    /** Favourites live in MediaStore from Android 11. */
    val canFavorite: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun share(item: MediaItem) {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(item.mimeType.ifEmpty { if (item.isVideo) "video/*" else "image/*" })
                .putExtra(Intent.EXTRA_STREAM, item.uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.share_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun toggleFavorite(item: MediaItem) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val request =
            MediaStore.createFavoriteRequest(context.contentResolver, listOf(item.uri), !item.isFavorite)
        launch(IntentSenderRequest.Builder(request.intentSender).build())
    }

    /** Moves to the trash from Android 11 (recoverable for 30 days), deletes before that. */
    fun delete(item: MediaItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request =
                MediaStore.createTrashRequest(context.contentResolver, listOf(item.uri), true)
            launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            try {
                context.contentResolver.delete(item.uri, null, null)
            } catch (e: RecoverableSecurityException) {
                launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
            }
        }
    }
}

@Composable
fun rememberMediaActions(): MediaActions {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    return remember(context) { MediaActions(context) { launcher.launch(it) } }
}
