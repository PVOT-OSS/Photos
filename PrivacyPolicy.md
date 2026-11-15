<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Privacy Policy — Photos

_Effective 15 November 2026_

Photos (package `org.prauga.photos`) is a gallery app published by Saalim Quadri for PVOT-OSS. This
policy explains what the app does with your data.

**In short: Photos has no internet access at all. It shows the pictures and videos already on your
phone, and nothing it reads ever leaves the phone through the app. There are no accounts, ads,
analytics or trackers.**

## What the app reads

| What | Why | Where it goes |
|---|---|---|
| Your photos and videos, through Android's media store | To show them in the library, albums and viewer | Nowhere: they are read and shown on the phone |
| Each photo's details: date, size, folder, resolution and the camera settings saved in the file (EXIF: camera model, aperture, exposure, ISO, focal length) | To sort the library by date, group albums and show the Info panel | Nowhere |
| Whether a photo is a favourite or in the trash | To show Favourites and keep deleted items out of the library | Nowhere |

The app does not read location from your photos, and it does not scan faces or the contents of
pictures.

## Permissions

| Permission | Used for |
|---|---|
| Photos and videos (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`; `READ_EXTERNAL_STORAGE` on Android 12 and older) | Showing your library |
| Selected photos only (`READ_MEDIA_VISUAL_USER_SELECTED`, Android 14 and newer) | Working with only the photos you choose, if you prefer that to full access |

The app does not request internet, location, camera, microphone, contacts or any other permission.
You can change or withdraw media access at any time in Android's settings; the app then shows only
what it is still allowed to see.

## Changes you make

**Favourite**, **Delete** and similar actions go through Android's own confirmation dialog and are
carried out by Android on the files themselves. On Android 11 and newer, deleted items go to the
system trash and can be restored for 30 days; on Android 10 they are deleted.

**Share** hands the photo or video you picked to the app you choose in Android's share sheet. What
that app does with it is covered by its own privacy terms.

## What the app stores

Photos keeps no database, account or copy of your pictures. It caches small thumbnails in memory
while it runs so scrolling stays smooth; they are gone when the app closes. The app has no data of
its own for Android backup to save.

## Children

The app is not directed at children and collects no data from anyone.

## Changes to this policy

If the app's behaviour changes, for example if a future version adds an optional cloud source, this
policy will be updated before that version is released, and the effective date above will change.

## Contact

Questions or concerns: open an issue at <https://github.com/PVOT-OSS/Photos/issues> or email
Saalim Quadri at danascape@gmail.com.
