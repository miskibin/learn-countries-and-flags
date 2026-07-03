# Poznaj Świat 🌍

Aplikacja na Androida do nauki krajów świata — ich **flag**, **stolic** i **położenia na mapie**. W całości po polsku, działa w pełni offline.

| Ekran główny | Quiz mapy | Mapa wiedzy | Oś czasu |
|---|---|---|---|
| ![Ekran główny](docs/screenshots/01_home.png) | ![Quiz mapy](docs/screenshots/04_map_quiz.png) | ![Mapa wiedzy](docs/screenshots/12_knowledge.png) | ![Oś czasu](docs/screenshots/11_timeline.png) |

## Funkcje

- **Powtórka dnia (spaced repetition)** — każde pytanie ma termin kolejnej powtórki (1 → 2 → 4 → 7 → 14 → 30 → 60 → 120 dni przy poprawnych odpowiedziach, reset przy błędzie). Codzienna sesja miesza flagi, mapę, stolice i historię naprzemiennie, a licznik serii dni motywuje do regularności.
- **Ścieżka nauki** — kontynenty opanowujesz po kolei (Europa → Azja → obie Ameryki → Afryka → Oceania); aplikacja rekomenduje następny i uruchamia przeplataną sesję (flagi + mapa + stolice + powiązana historia) dla tego regionu.
- **Mapa wiedzy** — świat pokolorowany Twoimi postępami (nowe / w trakcie / opanowane) z paskami postępu per kontynent; dotknięcie kraju otwiera jego szczegóły.
- **Quiz „Flagi"** — zgadnij kraj po fladze albo (losowo) flagę po kraju z siatki czterech flag.
- **Quiz „Mapa"** — wskaż podany kraj na interaktywnej mapie świata (przybliżanie, przesuwanie, 3 próby na pytanie, przycisk „Nie wiem — pokaż" z najazdem kamery na odpowiedź).
- **Quiz „Stolice"** — dopasuj stolicę do kraju.
- **Quiz „Historia"** w trzech formach — dopasuj rok, wskaż które z dwóch wydarzeń było wcześniej, albo połącz wydarzenie z krajem; po odpowiedzi mini-oś czasu pokazuje epokę oraz najbliższe wcześniejsze i późniejsze wydarzenie.
- **Oś czasu** — 150 najważniejszych wydarzeń historii świata (od rewolucji neolitycznej po XXI wiek) podzielonych na epoki, z powiązanymi krajami i dwuzdaniowym kontekstem.
- **XP, poziomy i nagrody** — punkty za każdą poprawną odpowiedź, bonus za ukończenie i perfekcyjną sesję, poziomy z paskiem postępu, seria poprawnych odpowiedzi („combo"), konfetti i wibracje (sukces/błąd/świętowanie) — pełny „game feel" w stylu Duolingo.
- **Przeglądaj kraje** — lista 197 krajów z wyszukiwarką (ignoruje polskie znaki), filtrem kontynentów, ciekawostką i mini-mapą dla każdego kraju. Ekran kraju pokazuje region (np. „Europa Środkowa"), klikalnych sąsiadów i wydarzenia historyczne związane z krajem — wiedza łączy się w całość.
- **Postępy nauki** — inteligentny dobór pytań (częściej pyta o to, czego jeszcze nie opanowałeś), seria 3 poprawnych odpowiedzi = materiał opanowany; statystyki na ekranie głównym.
- Po każdej odpowiedzi wyświetlana jest **ciekawostka** o kraju lub kontekst wydarzenia.
- Filtr kontynentów obowiązuje w trybach krajowych (Europa, Azja, Afryka, obie Ameryki, Oceania).
- Motyw jasny i ciemny, dynamiczne kolory Material You (Android 12+), ekran powitalny.

## Dane

- 197 krajów (członkowie ONZ + Watykan, Palestyna, Tajwan, Kosowo) z sąsiadami, regionami i mini-stronami wiki (geografia, historia, współczesność).
- 150 wydarzeń historycznych po polsku z mini-stronami wiki (tło i skutki), rozłożonych po epokach i kontynentach; sesje historyczne są jednotematyczne i chronologiczne, a kraje uczysz od największych.
- Nazwy krajów po polsku z zestawu [mledoze/countries](https://github.com/mledoze/countries) (licencja ODbL).
- Flagi z [flagcdn.com](https://flagcdn.com) (domena publiczna).
- Granice państw z [world.geo.json](https://github.com/johan/world.geo.json) (Natural Earth, domena publiczna), wstępnie przetworzone do kompaktowego formatu.
- Polskie nazwy stolic i ciekawostki wygenerowane na potrzeby aplikacji.

Wszystkie dane są wbudowane w aplikację — nie wymaga internetu ani żadnych uprawnień.

## Budowanie

Wymagany JDK 17+ oraz Android SDK (platforma 35).

```bash
./gradlew :app:assembleDebug      # APK debug
./gradlew :app:assembleRelease    # APK release (zminifikowany, ~2 MB)
./gradlew :app:testDebugUnitTest  # testy (Robolectric, ze zrzutami ekranu)
```

APK znajdziesz w `app/build/outputs/apk/`.

## Architektura

- Kotlin + Jetpack Compose (Material 3), jedna aktywność, Navigation Compose.
- Mapa świata rysowana na `Canvas` z własną projekcją i testem trafienia punkt-w-wielokącie — bez zewnętrznych bibliotek map.
- Postępy i harmonogram powtórek (SRS) zapisywane w DataStore (Preferences).
- Test dymny end-to-end na Robolectric przechodzi przez wszystkie główne ekrany i zapisuje zrzuty ekranu do `app/build/screenshots/`.
