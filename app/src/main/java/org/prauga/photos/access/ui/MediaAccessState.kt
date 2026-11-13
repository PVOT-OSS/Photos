// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.access.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Stable
class MediaAccessState internal constructor(initial: MediaAccess) {
    var access: MediaAccess by mutableStateOf(initial)
        internal set

    internal var launcher: (() -> Unit)? = null

    fun request() {
        launcher?.invoke()
    }
}

/** Re-read on every resume: partial access can change in Settings while the app is away. */
@Composable
fun rememberMediaAccessState(): MediaAccessState {
    val context = LocalContext.current
    val state = remember { MediaAccessState(context.mediaAccess()) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            state.access = context.mediaAccess()
        }
    SideEffect { state.launcher = { launcher.launch(mediaPermissions) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.access = context.mediaAccess() }
    return state
}
