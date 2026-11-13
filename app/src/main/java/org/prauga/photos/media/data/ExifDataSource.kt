// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.data

import android.content.ContentResolver
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.prauga.photos.di.IoDispatcher
import org.prauga.photos.media.model.CaptureDetails
import timber.log.Timber

class ExifDataSource
@Inject
constructor(
    private val resolver: ContentResolver,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun read(uri: String): CaptureDetails? =
        withContext(io) {
            try {
                resolver.openInputStream(uri.toUri())?.use { stream -> ExifInterface(stream).details() }
            } catch (e: IOException) {
                Timber.w(e, "Could not read EXIF")
                null
            } catch (e: SecurityException) {
                Timber.w(e, "No permission to read EXIF")
                null
            }
        }

    private fun ExifInterface.details(): CaptureDetails =
        CaptureDetails(
            camera =
                listOfNotNull(getAttribute(ExifInterface.TAG_MAKE), getAttribute(ExifInterface.TAG_MODEL))
                    .joinToString(" ")
                    .ifBlank { null },
            aperture = getAttribute(ExifInterface.TAG_F_NUMBER),
            exposureSeconds = getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull(),
            iso = getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY),
            focalLengthMm = getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 },
        )
}
