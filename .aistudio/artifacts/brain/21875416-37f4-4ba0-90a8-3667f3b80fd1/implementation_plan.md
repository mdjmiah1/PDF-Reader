# PDF Reader — Complete Android Application

A modern, high-performance Android PDF Reader built with Jetpack Compose, Material 3, Room persistence, and Android's native `PdfRenderer`. The app features a distraction-free document viewer, freehand and highlighter annotations, in-document search, page bookmarks, reading progress tracking, and pre-bundled starter guides for instant testing.

***

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed from your answers and reference designs:

- **Pre-loaded Sample Documents**: Confirmed. The app will bundle 3 realistic PDF documents ("UI Design Guide 2024", "AI Trends Report", "Design Systems Handbook") generated into internal app storage on first launch so you can immediately read, annotate, search, and bookmark without needing to copy files to the emulator.
- **Annotation & Markup**: Confirmed. Full freehand pen drawing, highlighter overlay with opacity control, and sticky page notes. Annotations are saved in Room by document ID and page index with full undo/redo and color picking.
- **In-Document Search**: Confirmed. Text search engine extracting page text with query highlighting, next/previous occurrence stepping, and direct page jumping.
- **Storage Access Framework (SAF)**: Native `ACTION_OPEN_DOCUMENT` picker with persistent URI permission granting to open any user-selected PDF from Downloads, Drive, or external storage.

***

## 1. Overview & Core Concept

- **What It Does**: A standalone, offline-capable PDF reader and document manager. Users can open local PDFs or SAF-picked documents, read with vertical continuous scrolling or page-fit zoom, highlight and draw notes, bookmark key pages, search text, and jump back into their last-read page seamlessly.
- **Target Audience**: Professionals, designers, researchers, and students who need a fluid, distraction-free reading experience with markup tools on mobile and tablet devices.
- **Key Value**: Native 60fps rendering performance with bounded bitmap memory caching, clean Material 3 UI modeled after the reference screens, and zero ads or paywalls.

***

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Color Palette**:
  - Primary Accent: Indigo `#4F46E5`
  - Secondary Accent: Violet `#7C3AED`
  - Canvas Background: Soft Paper Neutral `#EEF0F5` (Light), Deep Slate `#101114` (Dark)
  - Surface: `#FFFFFF` (Light), `#1B1D23` (Dark)
  - Elevated Cards: `#F7F8FC` (Light), `#242630` (Dark)
  - Text Primary: `#111827` (Light), `#F9FAFB` (Dark)
  - Text Secondary: `#6B7280` (Light), `#A1A1AA` (Dark)
- **Typography & Rhythm**:
  - Clean M3 typography scale with 8dp grid spacing and 16dp rounded corner radii.
  - Floating pill-shaped page counter and docked reading toolbars.

### Key Screens & User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                               HOME SCREEN                              │
├────────────────────────────────────────────────────────────────────────┤
│  [Logo] PDF Reader                     [Search]  [Avatar]              │
│  Good morning, Reader 👋                                              │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ 📖 Continue Reading                                              │  │
│  │ UI_Design_Guide_2024.pdf                                         │  │
│  │ Page 12 of 45 • 27% finished                                     │  │
│  │ [=========---------------------------------]  [ Continue -> ]    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  Quick Actions:                                                        │
│  [ 📂 Open PDF ]  [ 📄 Sample Docs ]  [ ⏱ Recent ]  [ 🔖 Bookmarks ]  │
│                                                                        │
│  Tabs: [ All Files ]  [ Recent ]  [ Starred ]  [ Sample ]              │
│  Document Cards with live thumbnail previews, size, and page stats.    │
│  Bottom Nav: [ Home ]  [ Files ]  [ Bookmarks ]  [ Settings ]          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Tap Document / Open PDF
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              READER SCREEN                             │
├────────────────────────────────────────────────────────────────────────┤
│  ← UI_Design_Guide_2024.pdf                       🔍   🔖   ⋮          │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                                                                │   │
│   │                     PDF PAGE CANVAS                            │   │
│   │         • High-res bitmap rendering via PdfRenderer            │   │
│   │         • Pinch-to-zoom (50% to 300%) & smooth scroll          │   │
│   │         • Annotation overlay (Pen / Highlighter / Notes)        │   │
│   │         • Search match highlight boxes                         │   │
│   │                                                                │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│                             [ 12 / 45 ]  <-- Jump-to-page pill         │
│                                                                        │
│  Floating Bottom Toolbar:                                              │
│  [ 🖌 Highlight ] [ ✏ Pen ] [ 📝 Note ] | [ - ] 100% [ + ] [ ⛶ Fit ]    │
└────────────────────────────────────────────────────────────────────────┘
```

- **Distraction-Free Mode**: Tapping the center of a page toggles fullscreen mode, hiding top and bottom toolbars smoothly.
- **Jump to Page**: Tapping the `12 / 45` pill opens a quick dialog with a slider and direct number input with real-time validation.
- **Search Modal**: In-document search drawer displaying all occurrence snippets, previous/next buttons, and automatic page navigation.

***

## 3. Key Product Decisions & Trade-Offs

- **Rendering Engine: Native `android.graphics.pdf.PdfRenderer` with Bitmap LRU Cache**:
  - *Rationale*: Zero third-party native binary bloat, 100% stable across Android 7.0+ (API 24+), privacy-respecting and offline.
  - *Memory Safety*: Uses an `LruCache<Int, Bitmap>` with a 64MB memory cap to recycle bitmaps as pages scroll offscreen, preventing OutOfMemoryErrors on 100+ page documents.
- **Local Persistence: Room Database + DataStore Preferences**:
  - *Rationale*: Instant local startup, fully reactive UI with Kotlin `Flow`, stores document metadata, reading positions, bookmarks, and vector annotations.
  - *DataStore*: Stores dark/light mode preference, default zoom mode (fit-width vs fit-page), and keep-screen-on toggles.
- **Vector Annotation Overlay Architecture**:
  - *Rationale*: Rather than destructively rewriting immutable PDF files on disk (which would risk corrupting user files and lose original digital signatures), annotations are recorded as normalized coordinate vector paths (`x/pageWidth`, `y/pageHeight`) stored in Room. They scale crisply at any zoom factor.
- **Sample Document Provisioning**:
  - Generates crisp vector-based PDF sample documents on first launch using Android's `PdfDocument` API into internal storage, so the app has immediate rich content upon opening.

***

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌───────────────────────────────────────────────────────────────────────┐
│                           UI Layer (Compose)                          │
│   HomeScreen ── ReaderScreen ── BookmarksScreen ── SettingsScreen     │
└───────────────────▲──────────────────────────────────▲────────────────┘
                    │                                  │
┌───────────────────┴───────────────────────┐   ┌──────┴──────────────┐
│                ViewModels                 │   │     PdfEngine       │
│  • HomeViewModel       • ReaderViewModel  │   │  • PdfRendererCore  │
│  • BookmarksViewModel  • SettingsViewModel│   │  • PageBitmapCache  │
└───────────────────▲───────────────────────┘   │  • TextSearchEngine │
                    │                           └─────────────▲───────┘
┌───────────────────┴───────────────────────┐                 │
│             Repository Layer              │                 │
│  • DocumentRepository • AnnotationRepo    │                 │
│  • PreferencesRepository                  │                 │
└─────────▲──────────────────────▲──────────┘                 │
          │                      │                            │
┌─────────┴─────────┐   ┌────────┴──────────┐   ┌─────────────┴───────┐
│   Room Database   │   │     DataStore     │   │ Android SAF & Files │
│  • DocumentEntity │   │  • ThemeMode      │   │  • ContentResolver  │
│  • BookmarkEntity │   │  • DefaultZoom    │   │  • App Internal Dir │
│  • AnnotationEntity   │  • KeepScreenOn   │   │  • Asset/Gen PDFs   │
└───────────────────┘   └───────────────────┘   └─────────────────────┘
```

### Room Database Entities
1. **`DocumentEntity`**:
   - `uriString` (Primary Key): Unique file URI or internal file path.
   - `title`: Extracted or sanitized filename.
   - `pageCount`: Total pages.
   - `fileSizeBytes`: Size in bytes.
   - `lastOpenedTimestamp`: Epoch time for sorting recent reads.
   - `lastReadPage`: Saved reading progress index (1-based).
   - `isFavorite`: Boolean flag for Starred tab.
   - `thumbnailPath`: Cached first-page thumbnail path.
2. **`BookmarkEntity`**:
   - `id`: Auto-generated ID.
   - `documentUri`: Foreign key to document.
   - `pageNumber`: Target page.
   - `title`: User bookmark note/title.
   - `createdAt`: Timestamp.
3. **`AnnotationEntity`**:
   - `id`: Auto-generated ID.
   - `documentUri`: Document identifier.
   - `pageNumber`: Page index.
   - `type`: `PEN`, `HIGHLIGHTER`, `NOTE`.
   - `colorHex`: Color value (e.g. Yellow `#FFFF00`, Indigo `#4F46E5`).
   - `strokeWidth`: Float value.
   - `pointsJson`: Normalized points string `[(x1,y1), (x2,y2)...]`.
   - `textNote`: Optional text note string.

***

## 5. Step-by-Step Implementation Sequence

1. **Dependencies & Version Catalog**:
   - Verify and activate `datastore-preferences`, `navigation-compose`, `room-runtime`, `room-ktx`, `room-compiler` (via KSP), and `compose-material-icons-extended`.
   - Update `app/build.gradle.kts` and configure app ID `com.aistudio.pdfreader.app`.
2. **App Branding & Custom Adaptive Launcher Icon**:
   - Update `metadata.json` and `res/values/strings.xml` to **PDF Reader**.
   - Create custom vector adaptive icon with modern book/document reader motif.
3. **Data Layer**:
   - Create Room Entities, DAOs (`DocumentDao`, `BookmarkDao`, `AnnotationDao`), and `PdfReaderDatabase`.
   - Implement `DocumentRepository` and `PreferencesRepository`.
   - Implement `SamplePdfGenerator` to build initial multi-page guides with realistic content, headers, diagrams, and searchable text.
4. **Native PDF Engine**:
   - Implement `PdfRendererCore` with background coroutine page rendering and thread-safe bitmap memory caching.
   - Implement `PdfSearchEngine` to locate words and return page numbers with context snippets.
5. **UI & Navigation**:
   - Setup `Theme.kt` with custom M3 color scheme (Indigo `#4F46E5` primary, light/dark neutral canvas).
   - Build `HomeScreen` with "Good morning" header, Continue Reading hero card, quick actions, tabbed document list, and bottom navigation.
   - Build `ReaderScreen` with:
     - Edge-to-edge top bar (back, title, search toggle, bookmark toggle, more menu).
     - Zoomable, scrollable PDF page canvas.
     - Interactive annotation overlay (draw pen paths, translucent highlighter, note markers).
     - Floating page indicator pill (`12 / 45`) with direct jump-to-page dialog.
     - Floating bottom reading toolbar (markup tool selector, zoom `-` / `%` / `+`, fit width).
     - In-document search drawer with query highlighting and match navigation.
   - Build `BookmarksScreen` with direct jump navigation and bookmark deletion.
   - Build `SettingsScreen` with theme toggle (System/Light/Dark), default zoom, screen-awake, and history clear.
6. **Compilation & Verification**:
   - Run `compile_applet` to verify zero build errors, check Robolectric tests, and ensure smooth runtime.
