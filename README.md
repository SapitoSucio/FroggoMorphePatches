# 👋🧩 Morphe Patches template

Template repository for Morphe Patches.

## ❓ About

Patches for apps I like.

<!-- TODO: Update this about section with a brief introduction/summary about this repo and what it offers. -->

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=SapitoSucio/FroggoMorphePatches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.3.1-dev.1](https://github.com/SapitoSucio/FroggoMorphePatches/releases/tag/v1.3.1-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;12 patches total
<details open>
<summary>📦 Facebook&nbsp;&nbsp;•&nbsp;&nbsp;11 patches</summary>
<br>

**🎯 Supported versions:**

| 573.0.0.37.74 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block Facebook Feed ads (573)](#block-facebook-feed-ads-573) | Blocks sponsored and promoted units in the Facebook 573 Feed without touching Reels or Stories. |  |
| [Block Facebook Reels ads (573)](#block-facebook-reels-ads-573) | Blocks sponsored Reels in the swipe feed plus Reels/video banners, video ads, and commercial breaks. |  |
| [Block Facebook Story ads (573)](#block-facebook-story-ads-573) | Filters Story ad buckets only at the concrete X68 provider return boundary. |  |
| [Block Facebook automatic refresh (573)](#block-facebook-automatic-refresh-573) | Experimental: blocks automatic foreground/hot-start/stale-tab/stale-post feed refresh while preserving cold initialization, manual, activity-result and fullscreen refresh paths. |  |
| [Change Facebook app theme (573)](#change-facebook-app-theme-573) | Adds AMOLED Black and Material You palettes while preserving Facebook's light/dark mode selection. | • Theme |
| [Download Facebook Media (573)](#download-facebook-media-573) | Adds direct downloads for the visible Story, Reel, and video media through MediaStore. | • Facebook image folder<br>• Facebook video folder |
| [Facebook 573 AI content diagnostics](#facebook-573-ai-content-diagnostics) | Logs Facebook GenAI structural flags for Feed stories without filtering them. |  |
| [Facebook 573 AI filter + recommendation diagnostics](#facebook-573-ai-filter-recommendation-diagnostics) | Filters detected AI Feed stories and logs structural metadata for DiscoverFeedUnit recommendation candidates in one bytecode injection. |  |
| [Facebook 573 Feed recommendation diagnostics](#facebook-573-feed-recommendation-diagnostics) | Logs structural metadata for injected Feed stories without filtering them. |  |
| [Hide Facebook AI content (573)](#hide-facebook-ai-content-573) | Filters Feed posts carrying Facebook's GenAI transparency metadata (Contenido de IA). |  |
| [Stop Facebook Story auto-advance (573)](#stop-facebook-story-auto-advance-573) | Leaves photo and video Stories on their completed frame until the viewer navigates manually. | • Loop Stories |

</details>

<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Clone app](#clone-app) | Changes the app package name to allow installing the same app multiple times. By default ".morphe" is appended to the package name. Each cloned install must use a unique package name. Cloning does not work with all apps and using this patch may cause app crashes or other unexpected behavior. | • Package name<br>• Update permissions<br>• Update providers |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

UserXYZ Patches are licensed under the [GNU General Public License v3.0](LICENSE)
