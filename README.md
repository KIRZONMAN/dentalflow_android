# DentalFlow (Android)

Cliente Android (Kotlin + XML) para el backend Node/Express + MongoDB.

## Requisitos
- Android Studio (Giraffe+)
- JDK 17

## Flavors
- **dev**: BASE_URL por defecto `http://10.0.2.2:3000/api`, API_KEY `demo-secret-change-me`
- staging / prod: placeholder (por definir)

## Configuración local
1. Levanta el backend en tu PC: `npm run dev`
2. Emulador Android Studio usa host `10.0.2.2`
3. Build Variant: **devDebug**

> Opcional: puedes sobreescribir BASE_URL o API_KEY añadiendo en `gradle.properties`:
>
> ```
> DEV_BASE_URL=http://10.0.2.2:3000/api
> DEV_API_KEY=demo-secret-change-me
> ```

## Ejecutar
- Compilar: `./gradlew :app:assembleDevDebug`
- Ejecutar desde Android Studio (devDebug).

## Convenciones
- Branches: `feature/`, `fix/`, `chore/`
- Commits: Conventional Commits
- PR: 1 reviewer requerido

## Licencia
MIT (ver `LICENSE`)
