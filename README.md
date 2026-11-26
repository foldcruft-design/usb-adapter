# OTG Drive

Android-приложение для автоматического обнаружения USB-флешек через OTG-адаптер и управления файлами.

## Сборка через GitHub Actions

В репозитории настроен workflow `.github/workflows/build.yml`:

- При каждом `push` или `pull_request` в ветку `main` / `master` (а также вручную во вкладке **Actions -> Run workflow**) запускаются тесты и сборка APK.
- Готовый `app-debug.apk` можно скачать в разделе **Actions -> последний запуск -> Artifacts -> OTG-Drive-debug-apk**.
- Для подписи релизной сборки (`assembleRelease` / `bundleRelease` по тегу `v*` или ручному запуску) можно добавить секреты репозитория (`Settings -> Secrets and variables -> Actions`):
  - `RELEASE_KEYSTORE_BASE64` — ваш `.jks`-файл в base64 (алиас ключа `upload`)
  - `STORE_PASSWORD` — пароль хранилища
  - `KEY_PASSWORD` — пароль ключа

## Локальная сборка

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Готовый APK появится в `app/build/outputs/apk/debug/app-debug.apk`.
