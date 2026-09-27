# Implementation Plan: Pantalla de Perfil (profile-screen)

## Overview

Este plan convierte el diseño de la pantalla de Perfil en pasos de codificación incrementales para la arquitectura real del proyecto (Android nativo, Java, Clean Architecture + MVVM + Hilt + Room, sin backend remoto). El orden respeta las dependencias reales: primero la capa de datos base (entidad + DAO), luego la migración de esquema, después validación y errores del dominio, el almacenamiento de foto, el dominio (interfaces + use cases), la implementación de datos, el cableado de Hilt, la presentación (ViewModel + Fragment + layout) y por último el cableado final y la verificación. Cada tarea construye sobre las anteriores y termina integrando el trabajo, sin código huérfano.

El lenguaje de implementación es Java (definido explícitamente en el diseño). El diseño incluye una sección de "Correctness Properties", por lo que se incluyen subtareas de pruebas basadas en propiedades (PBT) marcadas como opcionales con `*`. Los tests exigidos por el Requisito 16 (R16.1–R16.4) se mantienen como tareas requeridas.

## Tasks

- [x] 1. Capa de datos base: entidad y DAO
  - [x] 1.1 Añadir la columna `avatar_uri` a `UserEntity` y actualizar el mapeo a dominio
    - Añadir el campo `@ColumnInfo(name = "avatar_uri") private String avatarUri;` (nullable) con su getter/setter en `data.local.entity.UserEntity`
    - Incluir `avatarUri` en el constructor y en el factory `newUser(...)` sin romper llamadas existentes (por defecto `null`)
    - Actualizar `toDomainModel()` para poblar `avatarUrl` desde `avatar_uri` (antes `null`); si `UserEntityMapper.toDomain` mapea campo a campo, poblar también allí
    - _Requisitos: R9.1, R2.1_

  - [x] 1.2 Añadir las consultas de actualización y de unicidad excluyente a `UserDao`
    - Añadir `@Query("UPDATE users SET username = :username, email = :email WHERE id = :id") void updatePersonalData(String id, String username, String email);`
    - Añadir `@Query("UPDATE users SET password_algorithm = :alg, password_iterations = :iter, password_salt = :salt, password_hash = :hash WHERE id = :id") void updateCredentials(String id, String alg, int iter, String salt, String hash);`
    - Añadir `@Query("UPDATE users SET avatar_uri = :avatarUri WHERE id = :id") void updateAvatar(String id, String avatarUri);`
    - Añadir `existsByEmailExcludingId(String email, String selfId)` y `existsByUsernameExcludingId(String username, String selfId)` con `id <> :selfId`
    - _Requisitos: R6.1, R7.11, R9.4, R5.2_

- [x] 2. Migración de esquema Room v4→v5
  - [x] 2.1 Subir la versión de la base de datos a 5 y definir `MIGRATION_4_5`
    - Cambiar `@Database(..., version = 5, exportSchema = true)` en `data.local.database.GolIADatabase` (era 4)
    - Definir `public static final Migration MIGRATION_4_5` con `db.execSQL("ALTER TABLE users ADD COLUMN avatar_uri TEXT")`
    - Mantener `exportSchema = true` para regenerar `app/schemas/...GolIADatabase/5.json` al compilar
    - _Requisitos: R10.1, R10.3, R10.4, R10.5, R10.7_

  - [x] 2.2 Registrar `MIGRATION_4_5` en `di.DatabaseModule`
    - Añadir `MIGRATION_4_5` al `addMigrations(...)` junto a `MIGRATION_2_3` y `MIGRATION_3_4`, conservando `fallbackToDestructiveMigration()` como red de seguridad
    - _Requisitos: R10.2_

  - [x] 2.3 Escribir el test de migración v4→v5 (androidTest) análogo a `MatchMigrationTest`
    - Crear la BD en v4 con el `CREATE TABLE users` de v4, sembrar una fila de usuario, cerrar y ejecutar `runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)`
    - Verificar con `PRAGMA table_info(users)` que existe `avatar_uri`, que la fila se conserva sin cambios y que `avatar_uri` es NULL para cuentas previas
    - _Requisitos: R10.6, R16.2_

- [x] 3. Validación del dominio
  - [x] 3.1 Añadir la regla de longitud máxima 64 y `PASSWORD_TOO_LONG` a `RegistrationValidator`
    - Añadir `PASSWORD_MAX = 64` y, en `validatePassword`, tras `PASSWORD_TOO_SHORT`, comprobar `length() > PASSWORD_MAX` → nuevo `ValidationError.PASSWORD_TOO_LONG`
    - Preservar el orden actual (requerido → mín → máx → mayúscula → minúscula → dígito) y la ausencia de trim en password; aplica idéntico a registro y perfil
    - _Requisitos: R7.4, R15.2_

  - [x] 3.2 Crear `ProfileValidator` que delega en `RegistrationValidator`
    - Crear `domain.validation.ProfileValidator` con `@Inject` recibiendo `RegistrationValidator`
    - Exponer `validateUsername`, `validateEmail`, `validateNewPassword`, `validateConfirm` delegando en el validador base, y `validateProfile(username, email)` que compone solo los campos editables (sin `full_name`)
    - _Requisitos: R3, R4, R15.1, R15.2, R15.3, R15.4_

  - [x] 3.3 Escribir tests unitarios JVM de `ProfileValidator` usando el hook `isEmailPattern`
    - Subclase de test de `RegistrationValidator` que sobreescriba `protected isEmailPattern(String)` con un regex simple (sin `android.util.Patterns`)
    - Casos: username formato-antes-que-longitud, min 3, máx 30; email obligatorio/formato/vacío; password mín 8, máx 64, mayúscula/minúscula/dígito
    - _Requisitos: R16.1, R3, R4, R15.2_

  - [ ]* 3.4 Escribir prueba de propiedad para el orden formato-antes-que-longitud del username
    - **Property 2: El formato de username se evalúa antes que la longitud**
    - **Validates: Requisitos 3.3, 3.4, 3.5, 3.6, 15.2**

  - [ ]* 3.5 Escribir prueba de propiedad para complejidad y longitud de la nueva contraseña
    - **Property 3: Reglas de complejidad y longitud de la nueva contraseña (frontera de 64)**
    - **Validates: Requisitos 7.4, 15.2**

- [x] 4. Ampliar el contrato de errores `AuthError`
  - [x] 4.1 Añadir `SESSION_UNAVAILABLE` y `SESSION_CLEAR_FAILED` al enum `AuthError`
    - Añadir ambos valores a `domain.error.AuthError`, transportados vía `AuthException` dentro de `Result.Error`
    - _Requisitos: R7.6, R12.5, R13.6_

- [x] 5. Almacenamiento y procesamiento de la foto de perfil
  - [x] 5.1 Añadir la dependencia `androidx.exifinterface` al `build.gradle` del módulo `app`
    - Añadir `implementation 'androidx.exifinterface:exifinterface:...'` y confirmar disponibilidad de `FileProvider` vía `androidx.core:core`
    - _Requisitos: R8.6_

  - [x] 5.2 Configurar `FileProvider` para la captura de cámara
    - Declarar el `<provider>` de `androidx.core.content.FileProvider` con `authorities="${applicationId}.fileprovider"`, `exported=false`, `grantUriPermissions=true` en `AndroidManifest.xml` (main)
    - Crear `res/xml/file_paths.xml` exponiendo un subdirectorio de `filesDir` para el archivo destino de captura
    - _Requisitos: R8.3_

  - [x] 5.3 Implementar `PhotoStorage` (validación, anti-OOM, EXIF, downscale, escritura determinista)
    - Crear `data.local.PhotoStorage` `@Singleton` con `@Inject` recibiendo `@ApplicationContext Context`; método `Result<String> processAndStore(String userId, Uri source)`
    - Validar formato (`image/jpeg`, `image/png`, `image/webp`) y tamaño (≤ 10 MB) por `ContentResolver` → `VALIDATION_ERROR` si no procede
    - Decodificar con `inJustDecodeBounds`/`inSampleSize` (anti-OOM), corregir orientación EXIF, downscale a 512×512 conservando proporción (función pura de dimensiones), recomprimir a JPEG
    - Escribir en `filesDir` con nombre determinista `avatar_{userId}.jpg`, borrar el archivo anterior si cambia la extensión, y devolver la ruta absoluta; nunca persistir el `content://` de origen
    - _Requisitos: R8.6, R8.7, R8.8, R9.2, R9.3, R9.9_

  - [ ]* 5.4 Escribir prueba de propiedad para el cálculo de dimensiones del downscale
    - **Property 6: El downscale produce una imagen dentro de 512×512 conservando proporción**
    - **Validates: Requisitos 8.6**

  - [ ]* 5.5 Escribir prueba de propiedad para la validación de formato/tamaño de imagen
    - **Property 7: La validación de formato/tamaño de imagen es correcta**
    - **Validates: Requisitos 8.7, 8.8**

  - [ ]* 5.6 Escribir tests de instrumentación de `PhotoStorage` con 1–3 imágenes reales
    - Verificar decodificación, corrección EXIF, resultado ≤512×512, escritura determinista en `filesDir` y rechazo de formato/tamaño no soportado
    - _Requisitos: R8.6, R8.7, R8.8, R16.2_

- [x] 6. Dominio: interfaces, fuente de ranking y casos de uso
  - [x] 6.1 Definir la interfaz `ProfileRepository`
    - Crear `domain.repository.ProfileRepository` con `getCurrentUser`, `updatePersonalData`, `changePassword`, `updateAvatar`, todos síncronos devolviendo `Result`
    - _Requisitos: R1.2, R5, R6, R7, R9, R6.8_

  - [x] 6.2 Definir `RankingDataSource`, `RankingSnapshot` y `PlaceholderRankingDataSource`
    - Crear la interfaz `RankingDataSource.getRankingForCurrentUser()` y el value object inmutable `RankingSnapshot` (invariantes: `predictionsMade>=0`, `accuracyPercentage` 0..100, `totalPoints>=0`, `rankingPosition>=1`)
    - Crear `PlaceholderRankingDataSource` `@Singleton` con valores placeholder idénticos, campo por campo, a la bienvenida de Inicio
    - _Requisitos: R11.2, R11.3, R11.4, R11.5, R11.6_

  - [x] 6.3 Implementar los casos de uso que despachan al `@IoExecutor`
    - Crear `LoadProfileUseCase`, `UpdatePersonalDataUseCase`, `ChangePasswordUseCase`, `UpdateProfilePhotoUseCase`, `LogoutUseCase` con el mismo esqueleto que `LoginUseCase` (`@IoExecutor ExecutorService`, entrega por `Callback<T>`)
    - `UpdateProfilePhotoUseCase` invoca `PhotoStorage` y luego `updateAvatar`; `LogoutUseCase` invoca `PreferencesManager.clearTokens()` y devuelve `SESSION_CLEAR_FAILED` si falla, sin borrar el registro del usuario
    - _Requisitos: R1.2, R5, R6, R7, R9, R11, R12.2, R12.5, R12.6, R13.7_

- [x] 7. Capa de datos: implementación del repositorio
  - [x] 7.1 Implementar `ProfileRepositoryImpl` siguiendo el patrón de `LocalAuthRepositoryImpl`
    - Crear `data.repository.ProfileRepositoryImpl` `@Singleton` `@Inject` con `UserDao`, `PasswordHasher`, `PreferencesManager`, `PhotoStorage`
    - `getCurrentUser(id)`: `getById`; null → `PERSISTENCE_ERROR`; éxito → `Result.Success(User)`
    - `updatePersonalData`: normalizar (`trim`+`toLowerCase(Locale.ROOT)`, `@` inicial fuera, `""`→`null`), pre-check de unicidad excluyendo el propio id, `updatePersonalData`, mapear `SQLiteConstraintException` a `EMAIL_TAKEN`/`USERNAME_TAKEN` y otros a `PERSISTENCE_ERROR`, recargar y devolver `User`
    - `changePassword`: recuperar entidad (null → `SESSION_UNAVAILABLE`), `verify` en tiempo constante (falla → `INVALID_CREDENTIALS`), `hash` con salt nuevo, `updateCredentials`, nunca texto plano
    - `updateAvatar`: `updateAvatar` en DAO, recarga y devuelve `User`; fallos → `PERSISTENCE_ERROR`
    - _Requisitos: R5, R6, R7, R9_

  - [x] 7.2 Escribir el test del `ChangePasswordUseCase`/repositorio (verificación + re-hash + no texto plano)
    - Verificar que se comprueba la contraseña actual contra el `PasswordCredential` almacenado antes de aceptar el cambio y que la nueva se re-hashea con salt aleatorio nuevo
    - Verificar que ni la contraseña actual ni la nueva se exponen en texto plano en el `Result` ni en las columnas persistidas
    - _Requisitos: R16.3, R16.4, R7.11, R7.12_

  - [ ]* 7.3 Escribir prueba de propiedad para la normalización idempotente
    - **Property 1: Normalización idempotente de email y username**
    - **Validates: Requisitos 3.1, 4.1, 5.1, 5.5**

  - [ ]* 7.4 Escribir prueba de propiedad para la unicidad excluyendo al propio usuario
    - **Property 4: La unicidad excluye al propio usuario** (con `UserDao` in-memory o fake DAO)
    - **Validates: Requisitos 5.2, 5.3, 5.4**

  - [ ]* 7.5 Escribir prueba de propiedad para la verificabilidad y no exposición de texto plano
    - **Property 5: El cambio de contraseña conserva la verificabilidad y nunca persiste texto plano**
    - **Validates: Requisitos 7.11, 7.12**

- [x] 8. Checkpoint - Asegurar que compila y pasan los tests
  - Asegurarse de que todos los tests pasan; preguntar al usuario si surgen dudas.

- [x] 9. Inyección de dependencias (Hilt)
  - [x] 9.1 Añadir los bindings de `ProfileRepository` y `RankingDataSource` en `RepositoryModule`
    - Añadir `@Binds @Singleton ProfileRepository bindProfileRepository(ProfileRepositoryImpl impl)` y `@Binds @Singleton RankingDataSource bindRankingDataSource(PlaceholderRankingDataSource impl)`
    - Confirmar que `PhotoStorage`, `ProfileValidator`, use cases y `ProfileRepositoryImpl` se construyen por `@Inject` constructor sin `@Provides` adicionales
    - _Requisitos: R6.8, R11.3, R15.1_

- [x] 10. Presentación: eventos, ViewModel, layout y Fragment
  - [x] 10.1 Implementar `Event<T>` (SingleLiveEvent) de un solo consumo
    - Crear `presentation.util.Event<T>` con `getContentIfNotHandled()` (consume una vez) y `peekContent()` (no consume)
    - _Requisitos: R14.1, R14.2, R14.3_

  - [ ]* 10.2 Escribir prueba de propiedad para la entrega única del evento
    - **Property 8: Un evento de un solo consumo se entrega exactamente una vez**
    - **Validates: Requisitos 14.1, 14.2, 14.3**

  - [x] 10.3 Implementar `PerfilViewModel extends BaseViewModel`
    - `@HiltViewModel` con `@Inject` recibiendo los use cases, `ProfileValidator`, `RankingDataSource` y `PreferencesManager`
    - Exponer estado: `isLoading`/`errorMessage`/`isSuccess` (heredados), `ProfileFormState` (full_name solo lectura, correo, username, errores por campo), `avatarPreviewPath`, `RankingSnapshot`, y `LiveData<Event<...>>` de navegación/éxito/error
    - `load()` (valida sesión con `isLoggedIn`/`getUserId`, redirige a login o carga), `savePersonalData`, `changePassword`, `updatePhoto`, `logout`
    - Contador de intentos in-memory (5 fallos → bloqueo 60s, reinicio al recrear el VM), anti-doble-submit y timeout de 30s
    - _Requisitos: R1, R2, R7.6, R7.8, R7.10, R11, R12, R13.1, R13.2, R13.5, R14.4_

  - [x] 10.4 Crear el layout completo `fragment_perfil.xml` según el mockup
    - Avatar circular (`ShapeableImageView`) con insignia de lápiz superpuesta; nombre completo solo lectura; campos editables de correo y username con botón "Guardar cambios"
    - Sección de cambio de contraseña con campos separados y su botón; sección "Historial" con las cuatro métricas; botón "Cerrar sesión"; `ProgressBar` de carga
    - _Requisitos: R1.3, R2.1, R6, R7.1, R8.1, R11.1, R12.1, R13.1_

  - [x] 10.5 Añadir los recursos de string de error localizados
    - Añadir `error_username_taken`, `error_email_taken`, `error_current_password_incorrect`, `error_persistence`, `error_validation`, `error_session_unavailable`, `error_session_clear_failed` y los mensajes de timeout/campo
    - _Requisitos: R13.6_

  - [x] 10.6 Reescribir `PerfilFragment` (`@AndroidEntryPoint`, binding, launchers, mapeo de errores)
    - Reemplazar el placeholder: `@AndroidEntryPoint`, inflar `fragment_perfil.xml`, observar `LiveData` y los `Event<T>`
    - Lanzar `ActivityResultLauncher` de galería, cámara (con `FileProvider`) y permiso de cámara; gestionar denegación simple y permanente
    - Traducir `AuthError` a strings con `messageFor(ex)`, mostrar errores por campo con `TextInputLayout.setError`, `Toast.LENGTH_SHORT` en éxitos y conservar datos editados en error
    - _Requisitos: R1.3, R1.5, R1.7, R2, R3, R4, R6.4, R6.7, R7.13, R8.1, R8.2, R8.4, R8.5, R9.5, R9.6, R9.7, R11, R13.3, R13.4, R14_

- [x] 11. Cableado final y verificación
  - [x] 11.1 Cablear la navegación de logout a `LoginActivity` con flags de tarea
    - En el observador de `navigationEvent`, lanzar `LoginActivity` con `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` tras `clearTokens()` exitoso; permanecer en Perfil si falla (`SESSION_CLEAR_FAILED`)
    - _Requisitos: R12.4, R12.5_

  - [x] 11.2 Integrar la sección Historial con `RankingDataSource`
    - Poblar las cuatro métricas desde `RankingSnapshot` (formato de porcentaje con 1 decimal); estado neutro/placeholder sin error si no hay métricas
    - _Requisitos: R11.1, R11.2, R11.5_

  - [ ]* 11.3 Escribir tests JVM del contador de intentos/bloqueo 60s, timeout 30s y mapeo AuthError→string
    - Contador con reloj/scheduler inyectable y reinicio al recrear el VM; timeout con executor controlado; mapeo de cada `AuthError` a su recurso
    - _Requisitos: R7.10, R13.5, R13.6_

  - [ ]* 11.4 Escribir test de instrumentación de logout (`clearTokens`)
    - Verificar que tras logout no queda ninguno de `access_token, refresh_token, user_id, user_name, is_logged_in, token_expiry`
    - _Requisitos: R12.3, R16.2_

- [x] 12. Checkpoint final - Asegurar que compila y pasan los tests
  - Asegurarse de que todos los tests pasan; preguntar al usuario si surgen dudas.

## Notes

- Las tareas marcadas con `*` son opcionales (pruebas basadas en propiedades y algunos tests de instrumentación) y pueden omitirse para un MVP más rápido.
- Los tests exigidos por el Requisito 16 (3.3, 2.3, 7.2) NO están marcados como opcionales: son tareas requeridas.
- Cada tarea referencia requisitos específicos para trazabilidad.
- Los checkpoints permiten validación incremental.
- Las pruebas de propiedades validan las propiedades de corrección universales del diseño; los tests unitarios y de instrumentación cubren ejemplos, bordes e infraestructura (Room, `clearTokens`, `Bitmap`/EXIF).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "3.1", "4.1", "5.1", "5.2", "6.2", "10.1"] },
    { "id": 1, "tasks": ["2.1", "3.2", "5.3", "6.1", "10.2"] },
    { "id": 2, "tasks": ["2.2", "2.3", "3.3", "3.4", "3.5", "5.4", "5.5", "5.6", "6.3"] },
    { "id": 3, "tasks": ["7.1"] },
    { "id": 4, "tasks": ["7.2", "7.3", "7.4", "7.5", "9.1"] },
    { "id": 5, "tasks": ["10.3", "10.4", "10.5"] },
    { "id": 6, "tasks": ["10.6", "11.2"] },
    { "id": 7, "tasks": ["11.1", "11.3", "11.4"] }
  ]
}
```
