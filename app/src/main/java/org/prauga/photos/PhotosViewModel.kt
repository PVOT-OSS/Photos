// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.prauga.photos.access.ui.MediaAccess
import org.prauga.photos.media.data.MediaRepository

@HiltViewModel
class PhotosViewModel @Inject constructor(private val repository: MediaRepository) : ViewModel() {
    fun onEvent(event: PhotosEvent) {
        when (event) {
            is PhotosEvent.AccessChanged -> repository.setCanRead(event.access != MediaAccess.None)
        }
    }
}
