# WhereToWatch 🎬

WhereToWatch is an Android app that lets you search for movies and instantly see
where you can stream, rent or buy them in your country. The country is detected
automatically from your location, or you can pick it from a list.

This project was built as the final certification project for the
**[Coroutines Mastery](https://coroutinesmastery.com/)** course. Its main goal is to show
correct, real-world use of Kotlin Coroutines and Flow on Android: debounced search with
`flatMapLatest`, parallel requests with structured concurrency, limiting concurrency with
`Semaphore`, proper cancellation and exception handling, and testing with virtual time.

**Tech stack:** Kotlin · Coroutines & Flow · Jetpack Compose · MVVM · Hilt · Retrofit ·
kotlinx.serialization · Navigation Compose · DataStore · Coil · TMDB API

## Setup

1. Create a free account on [TMDB](https://www.themoviedb.org/) and open
   *Settings → API*.
2. Copy the **API Read Access Token** (the long one, not the short "API Key").
3. Add it to `local.properties` in the project root (this file is not committed):
   ```properties
   TMDB_API_TOKEN=eyJhbGciOiJIUzI1NiJ9...
   ```
4. Sync Gradle and run the `app` configuration.

Without a token the app still builds, but every search shows an "invalid token" error.

## Architecture

Single module, MVVM, split into three layers by package:

```
com.coroutine.mastery.wheretowatch
├── core     dispatchers (DI qualifiers), suspendRunCatching
├── data     Retrofit API, DTOs, mappers, repositories, DataStore, country detection, Hilt modules
├── domain   models, MovieRepository interface, use cases (pure Kotlin)
└── ui       navigation, SearchViewModel, Compose screens and components
```

- **data → domain ← ui.** The UI only knows the domain models and use cases. The
  repository returns `Result<T>` with an `AppError`, so Retrofit exceptions never reach
  the ViewModel.
- One screen for now (`SearchRoute`). Navigation uses type-safe routes, so a details screen
  can be added as `MovieDetailsRoute(movieId)`.

## How coroutines are used, and why

### Search pipeline: `SearchViewModel`

```
query ─ debounce ─ trim ─ distinctUntilChanged ─┐
                                                 combine ─ flatMapLatest(search) ─ scan(reduce) ─ stateIn
retry ───────────────────────────────────────────┘
```

| Operator | Why |
|---|---|
| `debounce { if (blank) 0 else 400ms }` | Waits until the user stops typing, so we don't send a request for every key. Clearing the field takes effect immediately. |
| `distinctUntilChanged` | `"inception "` and `"inception"` don't trigger two searches. |
| `combine(…, retryTrigger)` | **Retry** re-emits the same query, which restarts the pipeline. `retryTrigger` is a `MutableSharedFlow(replay = 1, DROP_OLDEST)`, so `tryEmit` never fails. |
| `flatMapLatest` | A new query **cancels** the running search *and all its watch-provider requests*. Results of an old query can never overwrite newer ones. |
| `scan(reduce)` | The search emits events (`Started`, `MoviesLoaded`, `ProvidersLoaded`, …). A pure `reduce()` function folds them into `SearchUiState`, and it is unit tested without coroutines. |
| `stateIn(Lazily)` | Starts with the first collector and keeps the state for the ViewModel's lifetime, so returning from the details screen (or from the background) doesn't search or detect the country again. The details screen uses `WhileSubscribed(5_000)` instead: leaving it should cancel its requests. |

### Parallel watch providers: `channelFlow` + `Semaphore`

After the search returns, the cards are shown immediately. Then the streaming providers
of **each** movie are loaded in parallel:

```kotlin
channelFlow {
    send(SearchEvent.MoviesLoaded(query, movies))
    val semaphore = Semaphore(4)
    movies.forEach { movie ->
        launch {
            val result = semaphore.withPermit { getWatchProviders(movie.id) }
            send(SearchEvent.ProvidersLoaded(movie.id, result))
        }
    }
}
```

- `channelFlow` lets several coroutines emit into the same flow, which plain `flow {}`
  doesn't allow.
- The `launch`ed requests are **children** of the `channelFlow` scope (structured
  concurrency). When `flatMapLatest` cancels the flow, they are all cancelled too. No
  `GlobalScope`, no leaks.
- `Semaphore(4)` caps how many requests run at the same time instead of firing 20 at once.
- A failure for one movie is a `Result.failure` value, not an exception, so only that card
  shows "Couldn't load streaming info" and the others are unaffected.

### Repository: `MovieRepositoryImpl`

- **Parallel decomposition:** the genre list and the search are independent, so they run in
  parallel with `async` inside `coroutineScope`. If the search fails, the genres request is
  cancelled automatically.
- **`Mutex`** protects the in-memory genre cache. It is held during the request, so two
  concurrent searches share one genres request instead of sending two. A failure isn't
  cached and doesn't fail the search: the cards are just shown without genres.
- **Main-safety:** every function is safe to call from the main thread. Work runs in
  `withContext(ioDispatcher)`, and the dispatcher is **injected** (`@IoDispatcher`) so
  tests can replace it.

### Pagination: infinite scroll

TMDB returns search results 20 at a time. The next page loads when the list gets close
to its end:

- **UI:** a `snapshotFlow` on the `LazyListState` emits when the last visible item is near
  the end and no page is loading. `distinctUntilChanged` on the item count asks only once
  for each new size.
- **One page at a time:** requests go through a `Channel(RENDEZVOUS)`. The search's
  `channelFlow` waits in `receive()` between pages, so `trySend` only succeeds when nothing
  is loading. Extra scroll events while a page loads are dropped instead of queued.
- **Same structured scope:** the page loop runs inside the search's `channelFlow`, so a
  new query (`flatMapLatest`) cancels the page that is loading, together with its provider
  requests. The new page's providers load in parallel under the same `Semaphore`.
- **Failures:** a failed page keeps the movies already shown, and the list footer shows
  Retry. The reducer drops duplicates, since TMDB pages can overlap.

### Country: Auto detection and the dropdown

The country only decides which streaming platforms are shown. Movie data and the UI stay
in English. It's chosen in the top bar: **Auto**, or a country from the list TMDB has
streaming data for (`watch/providers/regions`).

- **DataStore** saves the selection. `RegionPreferencesDataStore.selection` is a `Flow`
  that feeds the country detection:
  ```
  selection ─ combine(detectTrigger) ─ transformLatest(resolve) ─ stateIn ─ regionUiState
  ```
- **One shared `RegionViewModel`.** It's created in `AppNavHost`, outside the navigation
  destinations, so it's scoped to the activity. The search and details screens both show the
  same dropdown (`RegionPicker`) and read the same state. A change on one screen shows up on
  the other, and Auto detection runs once for the whole app.
- **The country doesn't touch the search pipeline.** TMDB's `watch/providers` response
  already contains every country, so each card keeps the providers for all of them
  (`ProvidersState.Loaded(byCountry)`) and the UI shows the entry for the selected country.
  Changing the country sends no request and updates the cards instantly. The movies and
  their providers don't wait for the country detection either.
- **`transformLatest`**: if the user picks a country while Auto is still detecting, the
  detection (location request, geocoder) is **cancelled**.
- **Auto** (`AndroidCountryDetector`) tries, in order: last known location → reverse
  geocoding → mobile network / SIM country → device locale → `US`. The UI shows where the
  country came from (e.g. "Romania · from your location").
- **Callback APIs become suspend functions** with `suspendCancellableCoroutine`:
  `LocationManagerCompat.getCurrentLocation` (with `invokeOnCancellation` cancelling the
  request through a `CancellationSignal`) and `Geocoder.GeocodeListener` on API 33+.
  Before API 33 the geocoder only has a blocking call, so it runs in `withContext(ioDispatcher)`.
- **`withTimeoutOrNull(10s)`** caps the location and geocoding steps. If time runs out,
  the work is cancelled and detection moves on to the next source.
- The platform `LocationManager` is used, so there's no dependency on Google Play Services.
  Only `ACCESS_COARSE_LOCATION` is requested, because a country doesn't need precise
  location. It's asked once at first launch, and again whenever the user picks Auto.

### Movie details: parallel sections with `combine`

Tapping a card opens `MovieDetailsRoute(movieId, regionCode)`. The screen needs five
independent requests: details, cast, trailer, recommendations, and watch providers.

```kotlin
combine(
    section { getMovieDetails(movieId) },
    section { getCast(movieId) },
    section { getTrailer(movieId) },
    section { getRecommendations(movieId) },
    providersSection(),
) { details, cast, trailer, recommendations, providers -> MovieDetailsUiState(...) }

fun <T> section(load: suspend () -> Result<T>) = flow {
    emit(Section.Loading)
    emit(load().fold({ Section.Loaded(it) }, { Section.Failed(it) }))
}
```

- `combine` collects every section's flow in its own coroutine, so **all requests run in
  parallel**. The tests check this with virtual time: the total time equals the slowest
  request, not the sum.
- It emits on every change, so **each section appears as soon as its own request is done**.
  With `async` and sequential `await`s, a fast section would wait for a slower one.
- Failures are values (`Section.Failed`), so **one failed section doesn't cancel the
  others**. Only a details failure shows the error screen with Retry. A failed secondary
  section is just hidden.
- Everything runs under `viewModelScope` via `stateIn(WhileSubscribed)`, so leaving the
  screen cancels the requests that are still running.
- Watch providers are cached in `MovieRepositoryImpl`, so opening a movie from the search
  shows its providers without another request.



- `runCatching` also catches `CancellationException`, which breaks cooperative cancellation.
  The project uses `suspendRunCatching`, which **rethrows** it and only wraps real failures.
- Every exception is mapped to an `AppError` (`Network`, `Unauthorized`, `Server`, `Unknown`)
  that the UI turns into a message.

### Lifecycle

- The UI collects with `collectAsStateWithLifecycle()`, so nothing is collected while the
  app is in the background.
- All work runs in `viewModelScope` and is cancelled when the ViewModel is cleared.

## Tests

```bash
./gradlew testDebugUnitTest
```

All tests use `kotlinx-coroutines-test` with **virtual time** (`runTest`,
`StandardTestDispatcher`, `advanceTimeBy`), so the 400 ms debounce and network delays run
instantly and deterministically. Flows are tested with **Turbine**.

| Test class | What it checks |
|---|---|
| `SearchViewModelTest` | next page appended with providers · requests while a page loads are **ignored** · nothing after the last page · failed page + retry · a new query **cancels** the loading page · a single search after the debounce · Loading → movies → providers · a new query **cancels** old provider requests · one failed card doesn't affect the others · at most 4 requests in parallel · error + retry · Empty · short query · clearing |
| `MovieRepositoryImplTest` | mapping · search and genres run **in parallel** (checked via virtual time) · genres fetched **once** under concurrent searches (`Mutex`) · genres failure tolerated · error mapping · **cancellation is propagated**, not turned into a failure · providers returned for every country · providers **cached** · details, cast, trailer and recommendations mapping |
| `RegionViewModelTest` | Auto resolves the detected country · choosing a country saves it · picking a country **cancels the Auto detection** · permission result re-detects · permission asked once · country list · switching screens doesn't detect again |
| `AndroidCountryDetectorTest` | full fallback chain · a slow location **times out** (virtual time) and is cancelled |
| `CountryRepositoryImplTest` | sorting · fetched once under concurrent calls · failures not cached |
| `RegionPreferencesDataStoreTest` | real DataStore on a temp file: Auto by default, save / restore |
| `ResolveRegionUseCaseTest` | Manual vs Auto |
| `MovieDetailsViewModelTest` | details show before slower sections · requests run **in parallel** (virtual time) · a failed section doesn't affect the others · details error + retry · leaving the screen **cancels** running requests |
| `SearchReducerTest` | the pure state reducer · picking a country's providers · next page appended without duplicates · failed page keeps the movies |
| `SuspendRunCatchingTest` | `CancellationException` is rethrown |

## Attribution

This product uses the TMDB API but is not endorsed or certified by TMDB.
Streaming availability data is provided by JustWatch.
