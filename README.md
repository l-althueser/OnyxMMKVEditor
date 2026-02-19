# Onyx (BOOX) MMKV Editor (Boox)

Minimal Android app to view and edit an MMKV store at `/onyxconfig/mmkv/onyx_config` on Boox devices. The MMKV store was added around Onyx Firmware version 4.0 and replaces the previous plain text storage in `/onyxconfig/eac_config`. The main advantage of the MMKV store in the current Firmware versions is that no ROOT access is required to read and write it, as long as this application is granted all-files access on Android 11+ (all recent Boox devices).

## WARNING
This application is intended for advanced users who understand the risks of modifying app configurations. Changing values without proper knowledge may lead to unexpected behavior or even render apps unusable. Always make sure to back up your data before making any changes.

## Features
- List or filter keys (app configs) from the MMKV store in `/onyxconfig/mmkv/onyx_config`
- Tap a key (app config) to view and edit its value
- Offer to copy or edit the value as text (some values are JSON, but MMKV storage treats them as strings without structure)
- Buildin backup and restore functionality for each key/value that was once edited with this app. You can always load the first overwritten config in the editor window and save (restore backup) if you want. However, better to keep a manual backup!
- Some keys (app configs) offer to add the handwriting optimization automatically, currently supported:
  - Obsidian (Ink, Excalidraw): `eac_app_md.obsidian` for view key `com.getcapacitor.CapacitorWebView`
  - DrawboardPDF: `eac_app_com.drawboard.pdf` for view key `com.getcapacitor.CapacitorWebView`
  - DrawNote: ` ` for view key `com.dragonnest.app.view.DrawingContainerView`
  - Ibis Paint: ` ` for view key `jp.ne.ibis.ibispaintx.app.glwtk.IbisPaintView`
  - Joplin 'Drawing' plugin: ` ` for view key `com.reactnativecommunity.webview.RNCWebView`
  - MediBang Paint: ` ` for view key `com.medibang.android.paint.tablet.ui.widget.CanvasView`
  - Penly: ` ` for view key `com.penly.penly.editor.views.EditorView`
  - Squid: ` ` for view key `com.steadfastinnovation.android.projectpapyrus.ui.widget.PageViewContainer`
  - Xodo: ` ` for view key `com.pdftron.pdf.PDFViewCtrl`

## Details about handwriting optimization
The handwriting optimization is a special configuration that can be added to the MMKV store for certain apps to improve the handwriting experience on Boox devices. It works by enabling specific settings that optimize the performance and responsiveness of handwriting input within those apps - mainly by adding an overlay that sends the input gradualy to the underlaying app.

**With information from https://gist.github.com/calliecameron/b3c62c601d255630468bd493380e3b7e:**

The entry for any app looks like a json file with a structure like:

```
{
	"appConfigMap": {
		"<app name>": ...
	}
}
```

where `<app name>` is the package name of the app. Take a look at the entries for OneNote (`com.microsoft.office.onenote`) or Evernote (`com.evernote`) for examples with handwriting optimisation.

If there is no entry for the app that you want to configure, open the app and change one of the optimisation settings -- doesn't matter which one -- to force the creation of an entry in the MMKV storage.

To enable handwriting optimisation, look for the following settings under your app:

```
"noteConfig": {
	...
	"enable": false,
	"globalStrokeStyle": {
		"enable": false,
		...
	},
	...
	"supportNoteConfig": false
	...
}
```

and change them as follows:

```
"noteConfig": {
	...
	"drawViewKey": "<your app's view name>",
	"enable": true,
	"globalStrokeStyle": {
		"enable": true,
		...
	},
	...
	"supportNoteConfig": true
	...
}
```

where `<your app's view name>` is different for different apps. Figuring out what this should be is the hard part, see below.

Reboot the device. Now, when you open the app, the 'handwriting' tab should appear in the optimisation settings. You can change other settings to further optimize the handwriting experience. The most important ones are:

* `repaintLatency`: how quickly the preview line changes to a real line when you stop drawing. 'Refresh time after lifting stylus' in the 'handwriting' tab.
* `globalStrokeStyle`
  * `strokeWidth`: 'Stroke width' in the 'handwriting' tab.
  * `strokeStyle`: values of 0, 1 and 2 used in the default config? Not sure what the difference is.
    * OneNote uses `1` for pens and `2` for highlighters.
  * `strokeColor`: two's complement of the ARGB colour, where A is always 255. To generate in python:

      ```python
      int.from_bytes(((255<<24) + (r<<16) + (g<<8) + b).to_bytes(4, byteorder=sys.byteorder, signed=False), byteorder=sys.byteorder, signed=True)
      ```

  * `strokeParams` and `strokeExtraArgs`: don't know. OneNote and Evernote don't set these.
* `styleMap`: somehow allows different pen styles in the app to appear differently when drawing the preview line. OneNote sets these, and the different pens there draw the preview line in different colours. But I don't know how to find the names to use as the keys, e.g. `pen_1` in OneNote.
  * `type: 0` appears to be the style -- different for pen and highlighter.
  * `type: 2` appears to be the colour.

## Finding the view key of an app

1. Pull the app's APK from the device (see e.g. https://stackoverflow.com/questions/4032960/how-do-i-get-an-apk-file-from-an-android-device).
  - `adb shell pm path com.app`
  - `adb pull /data/app/.../base.apk ./app.apk`
2. Open the APK in Android Studio via File → Profile or Debug APK.
3. Inspect the XML layout files under res/layout and look for view class names that appear related to drawing or canvas functionality (custom view classes or view ids that mention “draw”, “canvas”, “pen”, etc.).
4. Add a candidate view name as the `drawViewKey` in the MMKV entry (`eac_config`), reboot the device, and test the app. If it does not enable handwriting, repeat with the next promising class name until the correct view key is found.

## Quick start guide if you want to build it from source

1. Install Android Studio: https://developer.android.com/studio
2. Open this folder in Android Studio (`Open an existing project`).
3. Let Android Studio download Gradle and dependencies.
4. Build and run on your Boox device (enable developer mode + USB debugging).

## Permissions & notes
- If the app can't read `/onyxconfig/mmkv`, grant all-files access using the settings screen the app opens.