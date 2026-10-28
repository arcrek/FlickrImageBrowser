# Project Management Status Report: Team PR & Merge Audit

**Date:** 2026-10-26  
**Branch Audited:** `origin/main` (`722c46c`) + all remote branches & GitHub PRs (`#1`–`#8`)

## Executive Summary

Out of the **7 team members** defined in `docs/TEAM_WORKLOAD_PLAN.md`:
- **3 members (`M2`, `M5`, `M7`) have NOT opened a Pull Request** for their assigned feature branches (`M5` and `M7` have unmerged remote branches with completed work; `M2`'s branch was merged directly into `main` without a PR).
- **0 open Pull Requests are currently awaiting review/merge**, but **2 pushed remote branches (`M5` and `M7`) have not been submitted as PRs or merged into `main`**, and **1 closed PR (`#5` by `M4`)** was superseded by PR `#6`.
- **Member 1 (`arcrek`)** merged the API migration PRs (`#2`, `#4`), while the remaining M1 repository collection-management methods (`TODO 1.1`–`1.4`) are still unchecked on `main` (partially implemented inside `M7`'s unmerged branch).

---

## 1. Member-by-Member PR & Merge Status

| Member | GitHub User | Assigned Role | Remote Branch | PR Opened? | Review / Merge Status to `main` | Plan TODOs (`main`) |
|---|---|---|---|---|---|---|
| **M1: Đặng Đạt** | `arcrek` | Architecture, Repository & Concurrency Lead | `feat/migrate-to-pexels-api`, `feat/migrate-to-pixabay-api` | **Yes** (`#2`, `#4` — API migrations) | **Merged** (`#2`, `#4` merged). No separate PR yet for M1 `TODO 1.1–1.4`. | `0/4` (`[ ]` 1.1–1.4) |
| **M2: Phú Vinh** | `RalfRicardo` | Remote API, Networking & DTO Mapper | `origin/feat/m2-remote-api` (`e5b660d`) | **No PR opened** | **Merged without PR** (merged directly into `main` via commit `e0fa7e6` by `CamCamCoder`). | `4/4` (`[x]` 2.1–2.4) |
| **M3: Đức** | `duongduc017` | Room Database, Schema & Migration | `feat/m3-database-schema` (`6125b88`) | **Yes** (PR `#8`) | **Reviewed (`APPROVED`) & Merged** (`722c46c` on 2026-10-24). | `5/5` (`[x]` 3.1–3.5) |
| **M4: Tấn** | `CamCamCoder` | Search Discovery, Pagination & Grid UI | `origin/feat/m4-discovery-pagination-ui` (`07a383c`) | **Yes** (PR `#5` closed, PR `#6` merged) | **Reviewed (`APPROVED`) & Merged** (`e595d0c` on 2026-10-23). | `4/4` (`[x]` 4.1–4.4) |
| **M5: Hải Minh** | `HMMM-thinking` | Photo Detail, Metadata & Collection Assignment | `origin/feat/m5-Photo-Detail-Metadata-and-Collection` (`b26ab80`) | **No PR opened** | **NOT Reviewed / NOT Merged** (1 commit ahead of `main`, 10 behind). | `0/4` (`[ ]` 5.1–5.4) |
| **M6: Tuấn Vinh** | `haohan233-tvinh` | Offline Favorites, Local Search & Undo UX | `feat/m6-favorites` (`a02b5b0`) | **Yes** (PR `#7`) | **Reviewed (`APPROVED`) & Merged** (`fb6a0d4` on 2026-10-24). | `4/4` (`[x]` 6.1–6.4 in code; `[ ]` in doc) |
| **M7: Phương** | `phuongvuhoang082-bit` | Collections UI, Navigation Host & Espresso QA | `origin/m7-collection-navigation-and-espresso` (`2bb6ab7`) | **No PR opened** | **NOT Reviewed / NOT Merged** (2 commits ahead of `main`, 25 behind — branched from initial commit `a0e1e10`). | `0/4` (`[ ]` 7.1–7.4) |

---

## 2. Members Who Have NOT Opened a Pull Request

1. **Member 5 — Hải Minh (`HMMM-thinking`)**
   - **Branch:** `origin/feat/m5-Photo-Detail-Metadata-and-Collection` (head commit `b26ab80`, pushed `2026-10-24 14:29:18 +0700`: *"Work updated"*).
   - **Scope in branch:** 6 files changed (`+257 / -7`) — `DetailViewModel.java`, `PhotoDetailActivity.java`, `activity_photo_detail.xml`, `menu_photo_detail.xml`, `strings.xml`, `DetailViewModelTest.java`.
   - **Status:** Pushed to `origin`, **no PR created**, **not reviewed**, **not merged** into `main`.

2. **Member 7 — Vũ Hoàng Phương (`phuongvuhoang082-bit`)**
   - **Branch:** `origin/m7-collection-navigation-and-espresso` (head commit `2bb6ab7`, pushed `2026-10-24 00:51:13 +0700`: *"Add collection detail screens, resources and tests"* and `56a61ad`: *"Implement M7 collections, navigation, tests and UI polish"*).
   - **Scope in branch:** 2 commits across 30+ files (`+2,178 / -126`), including `CollectionsFragment`, `CollectionDetailFragment`, `MainActivityNavigationTest`, `CollectionsViewModelTest`, plus cross-package changes to `PhotoRepository`, `FlickrClient`, `SearchViewModel`, `FavoritesViewModel`, and `DetailViewModel`.
   - **Risk / Blocker:** Branched off the initial Flickr boilerplate commit (`a0e1e10`, 25 commits behind `origin/main`) before the Pixabay migration and M3/M4/M6 merges. Will have heavy merge conflicts and package-boundary overlaps unless rebased onto `origin/main`.
   - **Status:** Pushed to `origin`, **no PR created**, **not reviewed**, **not merged** into `main`.

3. **Member 2 — Phú Vinh (`RalfRicardo`)** *(Merged without PR)*
   - **Branch:** `origin/feat/m2-remote-api` (commit `e5b660d`, `2026-10-23 01:16:36 +0700`).
   - **Status:** Never opened a GitHub Pull Request; branch was merged directly into `main` via commit `e0fa7e6` by `CamCamCoder`. All 4 M2 TODOs are in `main`.

4. **Member 1 — Đặng Đạt (`arcrek`)** *(M1 Collection/Resource TODOs)*
   - Opened and merged PR `#2` and PR `#4` for the Pexels/Pixabay API migrations, but has not opened a dedicated M1 PR for `TODO 1.1` (pagination metadata in `Resource<T>`) and `TODO 1.3` (`removePhotoFromCollection`, `observePhotosInCollection`, `deleteCollection` in `PhotoRepository`).

---

## 3. Pull Requests & Branches Not Reviewed or Merged to `main`

### A. Open Pull Requests
- **None (`0` open PRs on GitHub).** Every PR that was opened (`#2`, `#4`, `#6`, `#7`, `#8`) has already been reviewed and merged, except `#5` which was closed.

### B. Closed / Unmerged Pull Requests
| PR # | Title | Author | Branch | Review State | Status |
|---|---|---|---|---|---|
| **#5** | `feat(m4): discovery pagination, responsive grid UI & modern Lumina aesthetic` | `CamCamCoder` (M4) | `feat/m4` | `REVIEW_REQUIRED` (0 reviews) | **CLOSED (Unmerged)** — Superseded by PR `#6` (`feat/m4-discovery-pagination-ui`), which was reviewed and merged. |

### C. Unmerged Remote Branches (Awaiting PR Creation, Review & Merge)
| Branch | Author | Commits Ahead / Behind `main` | Blocker / Next Action |
|---|---|---|---|
| `origin/feat/m5-Photo-Detail-Metadata-and-Collection` | `HMMM-thinking` (M5) | **+1 ahead / -10 behind** (based on `e595d0c`) | Open PR targeting `main`, rebase onto latest `main` (`722c46c`), run review & merge. |
| `origin/m7-collection-navigation-and-espresso` | `phuongvuhoang082-bit` (M7) | **+2 ahead / -25 behind** (based on `a0e1e10`) | Rebase onto `origin/main`, strip out stale Flickr/cross-member overrides (`FlickrClient.java`, `SearchViewModel`, `FavoritesViewModel`, `DetailViewModel`), keep M7 + required `PhotoRepository` collection methods, open PR, review & merge. |

---

## 4. Recommended Next Actions

1. **Open & Review Member 5 PR (`feat/m5-Photo-Detail-Metadata-and-Collection`):**
   - Clean, isolated diff (6 files in `ui/detail/*` and resources). Rebase onto `origin/main` and open a PR for review and merge.
2. **Rebase, Clean Up & Open Member 7 PR (`m7-collection-navigation-and-espresso`):**
   - Because `m7-collection-navigation-and-espresso` was branched off `a0e1e10` (pre-Pixabay migration) and modifies files owned by M2, M3, M4, M5, and M6, rebasing directly will conflict with PRs `#4`, `#6`, `#7`, and `#8`. Extract M7's `ui/collections/*`, `MainActivity`, `MainActivityNavigationTest`, `CollectionsViewModelTest`, and the missing M1 `PhotoRepository` collection methods onto `origin/main`, then open the PR.
3. **Sync `docs/TEAM_WORKLOAD_PLAN.md` Checkboxes:**
   - Mark Member 6 (`TODO 6.1–6.4`) as `[x]` (already merged in PR `#7`), and update M1, M5, and M7 checkboxes once their PRs land on `main`.
