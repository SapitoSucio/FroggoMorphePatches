# 👋🧩 Morphe Patches template

Template repository for Morphe Patches.

## ❓ About

Patches for apps I like.

<!-- TODO: Update this about section with a brief introduction/summary about this repo and what it offers. -->

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=SapitoSucio/FroggoMorphePatches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.2.0-dev.12](https://github.com/SapitoSucio/FroggoMorphePatches/releases/tag/v1.2.0-dev.12)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;9 patches total
<details open>
<summary>📦 Facebook&nbsp;&nbsp;•&nbsp;&nbsp;9 patches</summary>
<br>

**🎯 Supported versions:**

| 573.0.0.37.74 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Block Facebook Story ads (573)](#block-facebook-story-ads-573) | Pre-release experiment: filters Story ad buckets only at the concrete X68 provider return boundary. |  |
| [Block Facebook ads (573)](#block-facebook-ads-573) | Stops feed, Reels/video, and commercial-break ads without modifying the Story viewer pipeline. |  |
| [Block Facebook automatic refresh (573)](#block-facebook-automatic-refresh-573) | Suppresses lifecycle feed refresh while preserving explicit refresh paths. |  |
| [[Diag A1] Facebook 573 ads - CSR tail-load](#diag-a1-facebook-573-ads-csr-tail-load) | DEV diagnostic: only disables MainFeedCSRDataLoaderImpl async-ad tail-load dispatch. |  |
| [[Diag A2] Facebook 573 ads - CSR converter](#diag-a2-facebook-573-ads-csr-converter) | DEV diagnostic: only nulls the bZU Feed CSR response converter. |  |
| [[Diag A3] Facebook 573 ads - Async controller](#diag-a3-facebook-573-ads-async-controller) | DEV diagnostic: only replaces FeedAsyncAdsController output with an empty C6Ke. |  |
| [[Diag B] Facebook 573 ads - Final feed filter](#diag-b-facebook-573-ads-final-feed-filter) | DEV diagnostic: only filters SPONSORED/PROMOTION feed edges and MultiAds sponsored data. |  |
| [[Diag C] Facebook 573 ads - Reels/video](#diag-c-facebook-573-ads-reels-video) | DEV diagnostic: only disables Reels/video and commercial-break ad fetch/success paths. |  |
| [[Diag D] Facebook 573 Stories - publication lifecycle](#diag-d-facebook-573-stories-publication-lifecycle) | DEV diagnostic: logs Story bucket publication, cached replay and viewer notification without changing behavior. |  |

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
