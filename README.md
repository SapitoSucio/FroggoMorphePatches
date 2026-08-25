# 👋🧩 Morphe Patches template

Template repository for Morphe Patches.

## ❓ About

Patches for apps I like.

<!-- TODO: Update this about section with a brief introduction/summary about this repo and what it offers. -->

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=SapitoSucio/FroggoMorphePatches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.2](https://github.com/SapitoSucio/FroggoMorphePatches/releases/tag/v1.1.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 Facebook&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 573.0.0.37.74 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block Facebook ads (573)](#block-facebook-ads-573) | Stops feed, Story Ads, deferred/tail loads, and video commercial-break ads. |  |
| [Block Facebook automatic refresh (573)](#block-facebook-automatic-refresh-573) | Disables News Feed hot-start refresh while preserving manual pull-to-refresh. |  |
| [Log Facebook ad routes (573)](#log-facebook-ad-routes-573) | Opt-in Logcat route markers for feed, Reels/video, and Story Ads diagnostics. |  |

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
