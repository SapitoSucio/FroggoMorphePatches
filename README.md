# 👋🧩 Morphe Patches template

Template repository for Morphe Patches.

## ❓ About

Patches for apps I like.

<!-- TODO: Update this about section with a brief introduction/summary about this repo and what it offers. -->

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=SapitoSucio/FroggoMorphePatches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.2.0](https://github.com/SapitoSucio/FroggoMorphePatches/releases/tag/v1.2.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;4 patches total
<details open>
<summary>📦 Facebook&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 573.0.0.37.74 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block Facebook Feed ads (573)](#block-facebook-feed-ads-573) | Blocks sponsored and promoted units in the Facebook 573 Feed without touching Reels or Stories. |  |
| [Block Facebook Reels ads (573)](#block-facebook-reels-ads-573) | Blocks sponsored Reels in the swipe feed plus Reels/video banners, video ads, and commercial breaks. |  |
| [Block Facebook Story ads (573)](#block-facebook-story-ads-573) | Filters Story ad buckets only at the concrete X68 provider return boundary. |  |
| [Block Facebook automatic refresh (573)](#block-facebook-automatic-refresh-573) | Suppresses lifecycle feed refresh while preserving explicit refresh paths. |  |

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
