# Oracle — MRT-3 Commuter Guide (OSMDroid Edition)
### Android App · Kotlin + Jetpack Compose · OpenStreetMap

> **No Google Maps API key required.** This version uses [OSMDroid](https://github.com/osmdroid/osmdroid) backed by OpenStreetMap (Mapnik) tiles — completely free and open source.

---

## Project Structure

```
oracle-mrt3/
├── app/
│   ├── src/main/
│   │   ├── java/com/oracle/mrt3/
│   │   │   ├── MainActivity.kt
│   │   │   ├── OracleApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── model/Models.kt
│   │   │   │   └── repository/FirestoreRepository.kt
│   │   │   ├── di/AppModule.kt
│   │   │   ├── ui/
│   │   │   │   ├── components/StationPickerSheet.kt
│   │   │   │   ├── navigation/Navigation.kt
│   │   │   │   └── screens/
│   │   │   │       ├── SplashScreen.kt
│   │   │   │       ├── LoginScreen.kt
│   │   │   │       ├── MainScreen.kt
│   │   │   │       ├── HomeScreen.kt
│   │   │   │       ├── FareScreen.kt
│   │   │   │       ├── MapScreen.kt        ← OSMDroid (no API key!)
│   │   │   │       ├── EmergencyScreen.kt
│   │   │   │       ├── ProfileScreen.kt
│   │   │   │       └── AccountSettingsScreen.kt
│   │   │   ├── ui/theme/Theme.kt
│   │   │   └── viewmodel/
│   │   │       ├── AuthViewModel.kt
│   │   │       ├── TripViewModel.kt
│   │   │       ├── ProfileViewModel.kt
│   │   │       └── EmergencyViewModel.kt
│   │   ├── res/values/{strings,themes}.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json              ← REPLACE with real file
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── firestore.rules
```

---

## Setup — Only 2 external services needed (no Maps key!)

### 1. Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com) → create a project
2. Add an Android app — package name: **`com.oracle.mrt3`**
3. Download `google-services.json` → put it in the `app/` folder
4. Enable in Firebase Console:
   - **Authentication** → Sign-in methods → **Phone** + **Google**
   - **Firestore Database** → create (production or test mode)
5. Copy the `web_client_id` (type 3) from `google-services.json` into:
   `app/src/main/res/values/strings.xml` → `default_web_client_id`
6. Deploy `firestore.rules` via the Firebase Console → Firestore → Rules tab

### 2. OpenStreetMap / OSMDroid
**Nothing to configure.** OSMDroid downloads tiles from OpenStreetMap automatically.
It uses your `context.packageName` as the user-agent (already set in `MapScreen.kt`).

OSMDroid tiles are cached on device storage automatically. On first launch, the device
needs internet to fetch tiles; subsequent views of the same area work offline.

---

## Firestore Data Model

```
users/{uid}/
  profile/data          → { displayName, updatedAt }
  tripHistory/{id}      → { origin, destination, fare, isDiscounted, createdAt }
  contacts/{id}         → { name, phone }

stationStatuses/{name}  → { status: "clear" | "slight" | "heavy" | "offline" }

emergencyReports/{id}   → { type, station, description, hasPhoto, userId, status, createdAt }

feedback/{id}           → { userId, rating, comment, createdAt }
```

**Seed station statuses:** In Firestore Console, create `stationStatuses` collection.
Add one document per station — document ID = station name, field `status` = `"clear"`.

The 13 station names: North Avenue, Quezon Avenue, GMA Kamuning,
Araneta Center-Cubao, Santolan-Annapolis, Ortigas, Shaw Boulevard,
Boni, Guadalupe, Buendia, Ayala, Magallanes, Taft Avenue.

---

## Map Features (OSMDroid)

| Feature | Implementation |
|---------|---------------|
| Tile source | OpenStreetMap Mapnik (free, no key) |
| Station markers | `org.osmdroid.views.overlay.Marker` |
| Route polyline | Per-segment `Polyline` colored by Firestore status |
| User location | `MyLocationNewOverlay` + `GpsMyLocationProvider` |
| Lifecycle | `DisposableEffect` calls `onResume()` / `onPause()` / `onDetach()` |
| Compose bridge | `AndroidView { MapView(...) }` |

Polyline colors by station status:
- 🟢 `clear` → `#00A95C`
- 🟡 `slight` → `#FFC107`
- 🔴 `heavy` → `#EF4444`
- ⚫ `offline` → `#000000`

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Architecture | MVVM + StateFlow |
| Auth | Firebase Auth (Phone OTP + Google) |
| Database | Firebase Firestore |
| **Maps** | **OSMDroid 6.1.18 + OpenStreetMap** |
| Location | FusedLocationProviderClient |
| Images | Coil |
| DI | Hilt |
| Build | Gradle Kotlin DSL |

---

## Permissions

| Permission | Why |
|-----------|-----|
| `INTERNET` | OSM tile downloads, Firebase |
| `ACCESS_NETWORK_STATE` | OSMDroid tile cache decisions |
| `ACCESS_FINE_LOCATION` | Live location dot on map |
| `WRITE_EXTERNAL_STORAGE` (≤API29) | OSMDroid tile cache |
| `CAMERA` / `READ_MEDIA_IMAGES` | Emergency photo attachment |
| `CALL_PHONE` | Emergency hotline tap-to-call |

---

## Color Reference

| Token | Hex |
|-------|-----|
| Primary (Dark Green) | `#00703C` |
| Secondary (Light Green) | `#00A95C` |
| Background | `#F2F2F2` |
| Error | `#EF4444` |
| Gold Accent | `#FFD700` |
| Text Secondary | `#64748B` |
| Dark Card | `#16211C` |
