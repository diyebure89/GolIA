# Design Document — Pantalla de Perfil (profile-screen)

## Overview

Esta funcionalidad materializa la pantalla `PerfilFragment` de GolIA (Android nativo, Java, Clean Architecture + MVVM + Hilt + Room, sin backend remoto activo). Hoy `fragment_perfil.xml` es un marcador vacío y `PerfilFragment` extiende `BasePlaceholderFragment`. El diseño construye desde cero: avatar circular con insignia de lápiz, formulario de datos personales (correo y nombre de usuario editables; nombre completo en solo lectura), flujo separado de cambio de contraseña, sección "Historial" (ranking) alimentada por una fuente compartida con Inicio, y botón "Cerrar sesión".

El diseño se ancla estrictamente a la arquitectura real verificada leyendo el código: reutiliza `domain.common.Result<T>`, `domain.common.Callback<T>`, `domain.error.AuthError`/`AuthException`, `domain.security.PasswordHasher`/`PasswordCredential`, `domain.validation.RegistrationValidator`, `data.local.PreferencesManager`, `data.local.dao.UserDao`, `data.local.entity.UserEntity`, `data.local.database.GolIADatabase`, el patrón de `LocalAuthRepositoryImpl`, el despacho al `@IoExecutor` que ya usan `LoginUseCase`/`RegisterUseCase`, los bindings `@Binds @Singleton` de `RepositoryModule`, y `presentation.viewmodel.BaseViewModel`.

### Objetivos y mapeo a los 16 requisitos

| Objetivo de diseño | Requisitos cubiertos |
|---|---|
| Identificar y cargar al usuario actual desde la sesión local y la BD | R1, R2 |
| Editar y validar nombre de usuario y correo con reglas compartidas | R3, R4, R15 |
| Garantizar unicidad y normalización excluyendo al propio usuario | R5 |
| Persistir datos personales sin tocar columnas no editadas | R6 |
| Cambio de contraseña con verificación previa y re-hash | R7 |
| Selección de foto (galería/cámara) con procesamiento seguro | R8 |
| Persistencia y visualización de la foto en almacenamiento interno | R9 |
| Migración de esquema v4→v5 con nueva columna `avatar_uri` | R10 |
| Sección Historial con fuente de ranking compartida con Inicio | R11 |
| Cierre de sesión atómico y redirección al login | R12 |
| Estados de UI (carga/éxito/error/timeout) y contrato tipado | R13 |
| Retención de estado y eventos de un solo consumo | R14 |
| Verificación mediante pruebas automatizadas | R16 |

### Decisiones de diseño y su justificación

- **Repositorio del dominio dedicado (`ProfileRepository`)** en lugar de ampliar `AuthRepository`: el perfil tiene un contrato de operaciones distinto (lectura por id, update parcial, cambio de credenciales, avatar). Mantener interfaces cohesivas respeta la regla de dependencias de Clean Architecture y evita inflar `AuthRepository`. La implementación replica el patrón exacto de `LocalAuthRepositoryImpl` (síncrono, devuelve `Result`, normaliza, pre-chequea unicidad, mapea `SQLiteConstraintException`).
- **Los use cases despachan al `@IoExecutor` y entregan por `Callback<T>`**, igual que `LoginUseCase`. El `PerfilViewModel` no conoce threading; solo reacciona al `Result` con `postValue`.
- **`ProfileValidator` delega en `RegistrationValidator`** para paridad exacta con registro (R15). La regla nueva de longitud máxima de contraseña (64) se añade al validador compartido, no solo al de perfil.
- **`RankingDataSource` como interfaz de dominio con impl placeholder singleton** provista por Hilt, consumida por Inicio y Perfil sobre el mismo binding (R11).
- **`Event<T>` (SingleLiveEvent)** para navegación de logout, éxito y error de un solo consumo, y retención de estado del formulario/preview vía `PerfilViewModel` (sobrevive a rotación) (R14).

---

## Architecture

La pantalla es una vista delgada (`PerfilFragment`) que observa `LiveData` de `PerfilViewModel`. El ViewModel invoca use cases; cada use case despacha su trabajo bloqueante al `@IoExecutor` y devuelve un `Result` por `Callback`. Los use cases dependen solo de interfaces de dominio (`ProfileRepository`, `RankingDataSource`, `PasswordHasher`, `ProfileValidator`, `PreferencesManager`). `ProfileRepositoryImpl` (capa data) usa `UserDao`, `PasswordHasher`, `PreferencesManager` y `PhotoStorage`.

```mermaid
flowchart TD
    subgraph Presentation
        F[PerfilFragment<br/>bind layout mockup]
        VM[PerfilViewModel<br/>extends BaseViewModel<br/>LiveData estado + Event T]
    end
    subgraph Domain
        LPU[LoadProfileUseCase]
        UPD[UpdatePersonalDataUseCase]
        CPU[ChangePasswordUseCase]
        UPP[UpdateProfilePhotoUseCase]
        LOU[LogoutUseCase]
        PV[ProfileValidator]
        PR[(ProfileRepository<br/>interface)]
        RDS[(RankingDataSource<br/>interface)]
        PH[(PasswordHasher<br/>interface)]
    end
    subgraph Data
        PRI[ProfileRepositoryImpl]
        DAO[UserDao<br/>+ nuevas queries UPDATE]
        PM[PreferencesManager]
        PS[PhotoStorage<br/>EXIF/downscale/filesDir]
        RDSI[PlaceholderRankingDataSource<br/>@Singleton]
    end

    F -->|observa LiveData / Event| VM
    VM --> LPU & UPD & CPU & UPP & LOU
    VM -->|valida en main o IO| PV
    VM -->|Historial| RDS
    LPU & UPD & CPU & UPP --> PR
    CPU --> PH
    UPP --> PS
    LOU --> PM
    LPU -->|isLoggedIn/getUserId| PM
    PR -.impl.-> PRI
    RDS -.impl.-> RDSI
    PH -.impl.-> HasherImpl[PasswordHasher impl<br/>SecurityModule]
    PRI --> DAO
    PRI --> PH
    PRI --> PM
    PRI --> PS

    subgraph Inicio
        HOME[Bienvenida Inicio]
    end
    HOME -->|mismo binding Hilt| RDS

    LPU & UPD & CPU & UPP & LOU -.@IoExecutor.-> IO[[ExecutorService IO<br/>ExecutorModule]]
```

Notas de flujo:
- **Carga (R1/R2):** `PerfilFragment` (en `onViewCreated`/`onResume`) pide `load()`; el VM comprueba `PreferencesManager.isLoggedIn()`; si es válida obtiene `getUserId()` y llama `LoadProfileUseCase`, que corre en `@IoExecutor` y lee vía `ProfileRepository.getCurrentUser(id)`. Sesión inválida → `Event` de navegación a `LoginActivity`.
- **RankingDataSource compartido:** el mismo binding Hilt alimenta la bienvenida de Inicio y la `Historial_Section` (R11.3), garantizando valores idénticos campo por campo.

---

## Components and Interfaces

### Presentation

#### `PerfilFragment`
Reemplaza el placeholder actual; deja de extender `BasePlaceholderFragment` y pasa a un `Fragment` con `@AndroidEntryPoint`, inflando un `fragment_perfil.xml` completo que refleja el mockup:

- **Avatar circular con badge de lápiz** (p. ej. `ShapeableImageView` circular + `FloatingActionButton`/`ImageButton` de lápiz superpuesto). Al pulsar el lápiz muestra un selector con exactamente dos opciones: galería y cámara (R8.1).
- **Nombre completo (solo lectura):** `TextView` o `TextInputEditText` no editable poblado desde `full_name` (R2).
- **Campos editables:** `TextInputLayout`+`TextInputEditText` para correo y nombre de usuario, con un único botón "Guardar cambios" (R6). Los errores por campo se muestran con `setError` del `TextInputLayout` a partir del contrato tipado traducido a recurso de string.
- **Sección cambio de contraseña:** campos separados `Current_Password`, `New_Password`, `Confirm_New_Password` y su botón de envío (R7.1).
- **Sección "Historial":** cuatro métricas (pronósticos, aciertos %, puntos, ranking) (R11).
- **Botón "Cerrar sesión"** (R12.1).
- **Indicador de carga** (p. ej. `ProgressBar`) que se muestra antes de cada operación asíncrona (R1.11, R13).

Responsabilidades: observar `LiveData` (`isLoading`, `errorMessage`, form state, preview de foto) y los `LiveData<Event<...>>` de éxito/error/navegación; traducir `AuthError` a recursos de string localizados; lanzar los `ActivityResultLauncher` de galería, cámara y permiso de cámara; mostrar `Toast.LENGTH_SHORT` en éxitos (R6.4, R7.13). No accede a repositorios ni a la BD.

Permisos/cámara (R8.2–R8.5): usa `ActivityResultContracts.RequestPermission` para `CAMERA`, `TakePicture` con un `Uri` del `FileProvider`, y `GetContent`/`PickVisualMedia` para galería. Deniego simple → aviso "no se puede usar sin permiso"; denegación permanente (detectada con `shouldShowRequestPermissionRationale`==false tras denegar) → aviso "habilítalo en ajustes".

#### `PerfilViewModel extends BaseViewModel`
Inyectado con `@HiltViewModel` y constructor `@Inject` recibiendo los use cases, `ProfileValidator`, `RankingDataSource` y `PreferencesManager`.

Estado expuesto:
- Heredado de `BaseViewModel`: `LiveData<Boolean> isLoading`, `LiveData<String> errorMessage`, `LiveData<Boolean> isSuccess` (con `postValue`).
- `LiveData<ProfileFormState>`: `full_name` (solo lectura), correo, username, valores de errores por campo; se restaura tras rotación (R14.4).
- `LiveData<String> avatarPreviewPath` / `LiveData<Uri>`: preview de la foto seleccionada/persistida, retenido ante cambios de configuración (R14.4).
- `LiveData<RankingSnapshot> ranking`: métricas del Historial.
- `LiveData<Event<NavTarget>> navigationEvent`, `LiveData<Event<UiMessage>> successEvent`, `LiveData<Event<AuthError>> errorEvent`: eventos de un solo consumo (R14.1–R14.3).

Lógica:
- `load()`: valida sesión con `PreferencesManager.isLoggedIn()`; si falla → `navigationEvent = Event(LOGIN)` (R1.8/1.9/7.6). Si es válida, `LoadProfileUseCase.execute(userId, cb)`; `setLoading(true)` antes; en éxito rellena form + avatar; en error técnico → `errorEvent` (R1.10).
- `savePersonalData(username, email)`: valida con `ProfileValidator`; si inválido, publica errores por campo y no persiste (R15.4). Si válido, `setLoading(true)`, deshabilita el botón (anti-doble-submit R13.1/13.2) y llama `UpdatePersonalDataUseCase`.
- `changePassword(current, new, confirm)`: valida formato (R7.2–R7.5); comprueba bloqueo por intentos (ver abajo); `ChangePasswordUseCase`.
- `updatePhoto(sourceUri)`: `UpdateProfilePhotoUseCase`.
- `logout()`: `LogoutUseCase`; en éxito → `navigationEvent = Event(LOGIN)`.
- **Contador de intentos in-memory (R7.10):** campo `int failedPasswordAttempts` y `long lockedUntilMillis` propios del `PerfilViewModel` (por sesión de pantalla). A las 5 fallas consecutivas de `INVALID_CREDENTIALS`, bloquea 60s. Como vive en el ViewModel, se reinicia a cero al recrearse la vista/VM. Es mitigación de UX, no de seguridad.
- **Timeout 30s (R13.5):** cada operación programa un temporizador (p. ej. `Handler`/`ScheduledExecutorService`); si no llega respuesta, oculta carga, rehabilita el control y emite `errorEvent` de timeout.

### Domain

#### `ProfileRepository` (interfaz)
Todos los métodos son síncronos y devuelven `Result` directamente (patrón idéntico a `AuthRepository`); los use cases despachan al `@IoExecutor`.

```java
public interface ProfileRepository {
    Result<User> getCurrentUser(String id);                                  // R1.2, R2.1
    Result<User> updatePersonalData(String id, String username, String email); // R5, R6
    Result<Void> changePassword(String id, String currentPassword, String newPassword); // R7
    Result<User> updateAvatar(String id, String absolutePath);               // R9
}
```

#### Use cases (despachan al `@IoExecutor`, entregan por `Callback<T>`)
Mismo esqueleto que `LoginUseCase`:

- `LoadProfileUseCase.execute(String userId, Callback<User>)` — lee vía `getCurrentUser` (R1.2).
- `UpdatePersonalDataUseCase.execute(String id, String username, String email, Callback<User>)` (R6).
- `ChangePasswordUseCase.execute(String id, String current, String neu, Callback<Void>)` (R7).
- `UpdateProfilePhotoUseCase.execute(String id, Uri source, Callback<User>)` — invoca `PhotoStorage` para procesar/copiar y luego `updateAvatar` (R8/R9).
- `LogoutUseCase.execute(Callback<Void>)` — invoca `PreferencesManager.clearTokens()`; si algo falla, devuelve `Result.Error(new AuthException(SESSION_CLEAR_FAILED))` (R12.5). No elimina el registro del usuario (R12.6).

```java
public class ChangePasswordUseCase {
    private final ProfileRepository repository;
    private final ExecutorService executor;
    @Inject public ChangePasswordUseCase(ProfileRepository repository, @IoExecutor ExecutorService executor) {
        this.repository = repository; this.executor = executor;
    }
    public void execute(String id, String current, String neu, Callback<Void> cb) {
        executor.execute(() -> cb.onResult(repository.changePassword(id, current, neu)));
    }
}
```

#### `ProfileValidator`
Componente del dominio con `@Inject` que **delega** en `RegistrationValidator` (paridad exacta, R15.2). Expone solo los campos del perfil:

```java
public class ProfileValidator {
    private final RegistrationValidator base;
    @Inject public ProfileValidator(RegistrationValidator base) { this.base = base; }

    public ValidationError validateUsername(String username) { return base.validateUsername(username); }
    public ValidationError validateEmail(String email)       { return base.validateEmail(email); }
    public ValidationError validateNewPassword(String pwd)   { return base.validatePassword(pwd); }
    public ValidationError validateConfirm(String pwd, String confirm) { return base.validateConfirm(pwd, confirm); }

    // Compone solo los campos editables del perfil (sin full_name).
    public ValidationResult validateProfile(String username, String email) { /* build con Field.USERNAME, EMAIL */ }
}
```

**Cambio en `RegistrationValidator` (compartido, R7.4/R15.2):** añadir `PASSWORD_MAX = 64` y, en `validatePassword`, tras `PASSWORD_TOO_SHORT`, comprobar `password.length() > PASSWORD_MAX` → nuevo `ValidationError.PASSWORD_TOO_LONG`. Se preserva el orden actual (requerido → mín → **máx** → mayúscula → minúscula → dígito) y la ausencia de trim en password. Esto aplica idéntico a registro y perfil.

#### `RankingDataSource` (interfaz) + `RankingSnapshot`
```java
public interface RankingDataSource {
    RankingSnapshot getRankingForCurrentUser(); // placeholder consistente con Inicio (R11.4)
}
```
`RankingSnapshot` (inmutable): `int predictionsMade` (>=0), `float accuracyPercentage` (0..100, 1 decimal al formatear), `int totalPoints` (>=0), `int rankingPosition` (>=1) (R11.2). Impl `PlaceholderRankingDataSource` `@Singleton` con valores placeholder idénticos a los de la bienvenida de Inicio; preparada para conectar el ranking real sin tocar `Perfil_UI` (R11.5/11.6).

#### Ampliación de `AuthError`
Añadir dos valores al enum existente (R7.6, R12.5, R13.6):
```java
public enum AuthError {
    USERNAME_TAKEN, EMAIL_TAKEN, INVALID_CREDENTIALS, PERSISTENCE_ERROR, VALIDATION_ERROR,
    SESSION_UNAVAILABLE,   // no hay sesión válida al operar
    SESSION_CLEAR_FAILED   // fallo al cerrar sesión
}
```
Viajan dentro de `Result.Error` vía `AuthException`; la UI los extrae con `((AuthException) ex).getAuthError()` y los mapea a string.

### Data

#### `ProfileRepositoryImpl` (`@Singleton`, `@Inject`)
Implementa `ProfileRepository`. Constructor: `UserDao userDao, PasswordHasher passwordHasher, PreferencesManager preferencesManager, PhotoStorage photoStorage`. Sigue **exactamente** el patrón de `LocalAuthRepositoryImpl`:

- **Normalización** con los mismos helpers: `normalize(v) = v.trim().toLowerCase(Locale.ROOT)` y `normalizeUsernameOrNull(v)` (retira un `@` inicial, `""` → `null`) (R5.1, R5.5).
- `getCurrentUser(id)`: `userDao.getById(id)`; si null → `Result.Error(AuthException(PERSISTENCE_ERROR))` para que la capa superior distinga "no existe" (combinado con la validación de sesión que redirige a login en R1.8). Éxito → `Result.Success(UserEntityMapper.toDomain(entity))`.
- `updatePersonalData(id, username, email)`:
  1. Normaliza email y username.
  2. **Pre-check de unicidad excluyendo al propio id:** usa consultas que excluyen `id` (ver DAO abajo). Conflicto → `EMAIL_TAKEN`/`USERNAME_TAKEN` (R5.2/5.3/5.4).
  3. Ejecuta `userDao.updatePersonalData(id, normUsername, normEmail)`.
  4. Captura `SQLiteConstraintException` y la discrimina con un `resolveConstraint` análogo (por email/username) → `EMAIL_TAKEN`/`USERNAME_TAKEN`; otros fallos → `PERSISTENCE_ERROR` (R5.6, R6.5/6.6). En error, no deja datos parciales (el UPDATE es atómico).
  5. Devuelve `Result.Success` con el `User` recargado por `getById`.
- `changePassword(id, current, neu)`:
  1. Recupera el `UserEntity` por id; si null → `Result.Error(AuthException(SESSION_UNAVAILABLE))` (R7.6).
  2. `passwordHasher.verify(entity.toCredential(), current)` (tiempo constante). Falla → `INVALID_CREDENTIALS`, sin tocar la BD (R7.9).
  3. `PasswordCredential neuCred = passwordHasher.hash(neu)` (salt aleatorio nuevo, R7.11).
  4. `userDao.updateCredentials(id, alg, iter, salt, hash)` — solo columnas de credencial (R7.11). Nunca persiste texto plano (R7.12).
  5. Fallo técnico → `PERSISTENCE_ERROR`.
- `updateAvatar(id, absolutePath)`: `userDao.updateAvatar(id, absolutePath)`; recarga y devuelve `User`. Fallos → `PERSISTENCE_ERROR` (R9.8).

#### `UserDao` — nuevas consultas
Añadir a la interfaz existente (junto a insert/find/exists/getById), tal como especifican R6.1, R7.11 y R9.4:
```java
@Query("UPDATE users SET username = :username, email = :email WHERE id = :id")
void updatePersonalData(String id, String username, String email);

@Query("UPDATE users SET password_algorithm = :alg, password_iterations = :iter, "
     + "password_salt = :salt, password_hash = :hash WHERE id = :id")
void updateCredentials(String id, String alg, int iter, String salt, String hash);

@Query("UPDATE users SET avatar_uri = :avatarUri WHERE id = :id")
void updateAvatar(String id, String avatarUri);

// Unicidad excluyendo al propio id (R5.2)
@Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email AND id <> :selfId)")
boolean existsByEmailExcludingId(String email, String selfId);

@Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username AND id <> :selfId)")
boolean existsByUsernameExcludingId(String username, String selfId);
```

#### `UserEntity` — nueva columna
Añadir columna nullable y actualizar el mapeo a dominio:
```java
@ColumnInfo(name = "avatar_uri")
private String avatarUri;   // NULLABLE: ruta absoluta en filesDir, o NULL
// getter/setter avatarUri; incluir en el constructor y en newUser(...)

public User toDomainModel() {
    return new User(id, fullName, username, email,
                    null,        // country
                    avatarUri,   // avatarUrl <- avatar_uri (antes null)
                    0, 0, 0, String.valueOf(createdAt));
}
```
`UserEntityMapper.toDomain(entity)` (existente) se apoya en `toDomainModel()`, por lo que hereda el cambio; si mapea campo a campo, se actualiza para poblar `avatarUrl` desde `avatar_uri`.

#### `GolIADatabase` — versión 5 y migración
```java
@Database(entities = { ... }, version = 5, exportSchema = true)  // era 4
...
public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
    @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("ALTER TABLE users ADD COLUMN avatar_uri TEXT");
    }
};
```
Registrar en `di.DatabaseModule` junto a las existentes:
```java
.addMigrations(GolIADatabase.MIGRATION_2_3, GolIADatabase.MIGRATION_3_4, GolIADatabase.MIGRATION_4_5)
```
Se mantiene `exportSchema = true`; el build regenera el JSON de esquema v5 en `app/schemas/...GolIADatabase/5.json` (R10.5). `fallbackToDestructiveMigration()` permanece como red de seguridad para saltos no cubiertos, pero la ruta 4→5 es explícita y no destructiva (R10.3/10.7).

#### `PhotoStorage` (`@Singleton`, `@Inject`)
Encapsula el procesamiento y la escritura de la foto en almacenamiento interno. Constructor recibe `@ApplicationContext Context`. Métodos:
```java
// Valida formato/tamaño, decodifica acotando dimensiones (anti-OOM),
// corrige EXIF, downscale a 512x512 conservando proporción, recomprime a JPEG,
// escribe en filesDir con nombre determinista y borra el anterior.
Result<String> processAndStore(String userId, Uri source); // devuelve ruta absoluta
```
Detalles:
- **Validación (R8.7/8.8):** acepta solo `image/jpeg`, `image/png`, `image/webp`; rechaza tamaño > 10 MB → `Result.Error(AuthException(VALIDATION_ERROR))` (la UI lo traduce a aviso de motivo). Formato/tamaño se detectan por `ContentResolver` (MIME + `openAssetFileDescriptor`/columna de tamaño) sin decodificar toda la imagen.
- **Decodificación anti-OOM (R8.6):** `BitmapFactory.Options` con `inJustDecodeBounds=true` para leer dimensiones, cálculo de `inSampleSize`, luego decodificación real acotada.
- **EXIF (R8.6):** `androidx.exifinterface.media.ExifInterface` para leer la orientación y rotar el bitmap.
- **Downscale 512×512 conservando proporción (R8.6):** función pura de cálculo de dimensiones (ver Correctness Properties) + `Bitmap.createScaledBitmap`.
- **Escritura (R9.3):** nombre determinista `avatar_{userId}.jpg` dentro de `context.getFilesDir()`; recomprime con `Bitmap.compress(JPEG, quality)`; devuelve `file.getAbsolutePath()`. **Nunca** persiste el `content://` de origen (R9.2).
- **Borrado del anterior (R9.9):** como el nombre es determinista por `userId`, la reescritura del mismo archivo evita huérfanos; si cambiara la extensión, borra el archivo previo referenciado en `avatar_uri`.

#### `FileProvider` para la cámara (R8.3)
- `AndroidManifest.xml` (main): declarar el provider
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
               android:resource="@xml/file_paths"/>
</provider>
```
- `res/xml/file_paths.xml`: exponer un subdirectorio de `filesDir` para el destino temporal de captura de cámara.

### DI (Hilt)
- `RepositoryModule` (abstract, `@Binds @Singleton`): añadir
```java
@Binds @Singleton public abstract ProfileRepository bindProfileRepository(ProfileRepositoryImpl impl);
@Binds @Singleton public abstract RankingDataSource bindRankingDataSource(PlaceholderRankingDataSource impl);
```
- `DatabaseModule`: registrar `MIGRATION_4_5` (arriba). `UserDao` ya se provee.
- `ExecutorModule`/`SecurityModule`/`qualifier.IoExecutor`: sin cambios; los use cases inyectan `@IoExecutor ExecutorService` y `PasswordHasher` como hoy.
- `PhotoStorage`, `ProfileValidator`, use cases y `ProfileRepositoryImpl` usan `@Inject` constructor; Hilt los construye sin `@Provides` adicionales.

---

## Data Models

### Nueva columna `avatar_uri`
Tabla `users`: `avatar_uri TEXT` nullable. Almacena la RUTA ABSOLUTA del archivo copiado a `filesDir`; NULL cuando no hay foto o para cuentas anteriores a la migración (R9.1, R10.4). Nunca almacena `content://`.

### `RankingSnapshot` (dominio, inmutable)
| Campo | Tipo | Invariante |
|---|---|---|
| `predictionsMade` | `int` | `>= 0` |
| `accuracyPercentage` | `float` | `0..100`, `<= 1` decimal al formatear |
| `totalPoints` | `int` | `>= 0` |
| `rankingPosition` | `int` | `>= 1` |

### `ProfileFormState` (presentación, inmutable) y preview
Retenidos en `PerfilViewModel` para sobrevivir a cambios de configuración (R14.4):
| Campo | Tipo | Notas |
|---|---|---|
| `fullName` | `String` | solo lectura; `""` si NULL (nunca "null", R1.5) |
| `email` | `String` | editable |
| `username` | `String` | editable; vacío = sin username |
| `emailError` / `usernameError` | `ValidationError`/`Integer` | error por campo, o null |
| `avatarPreviewPath` | `String`/`Uri` | ruta actual o preview seleccionado |

### `Event<T>` (SingleLiveEvent)
Envuelve un contenido consumible exactamente una vez (R14):
```java
public final class Event<T> {
    private final T content;
    private boolean handled = false;
    public Event(T content) { this.content = content; }
    public T getContentIfNotHandled() { if (handled) return null; handled = true; return content; }
    public T peekContent() { return content; }   // no consume
}
```
Usado para `NavTarget` (LOGIN), `UiMessage` (éxito) y `AuthError` (error). El `PerfilFragment` observa y actúa una sola vez, evitando repetir navegación/mensajes tras rotación (R14.2/14.3).

---

## Correctness Properties

*Una propiedad es una característica o comportamiento que debe cumplirse en todas las ejecuciones válidas del sistema: un enunciado formal sobre lo que el sistema debe hacer. Las propiedades tienden un puente entre la especificación legible por humanos y las garantías de corrección verificables por máquina.*

Estas propiedades se implementarán con una librería de PBT para JVM (p. ej. **jqwik** para JUnit 5, o **junit-quickcheck** para JUnit 4), con **mínimo 100 iteraciones** por propiedad. Las áreas de infraestructura (migración Room, `clearTokens`) se cubren con tests de integración/instrumentación (ver Testing Strategy), no con PBT.

### Property 1: Normalización idempotente de email y username

*Para todo* string de entrada, aplicar la normalización del `ProfileRepositoryImpl` (trim + `toLowerCase(Locale.ROOT)`, y para username retirar un único `@` inicial con `""`→`null`) dos veces produce el mismo resultado que aplicarla una vez, y el resultado no contiene espacios en los extremos, está en minúsculas y (username) no empieza por `@`.

**Validates: Requirements 3.1, 4.1, 5.1, 5.5**

### Property 2: El formato de username se evalúa antes que la longitud

*Para todo* username normalizado no vacío que contenga al menos un carácter fuera de `[A-Za-z0-9_.]`, `ProfileValidator.validateUsername` devuelve siempre el error de FORMATO, con independencia de su longitud; y *para todo* username cuyos caracteres pertenezcan al conjunto con longitud entre 3 y 30 el resultado es válido (null).

**Validates: Requirements 3.3, 3.4, 3.5, 3.6, 15.2**

### Property 3: Reglas de complejidad y longitud de la nueva contraseña

*Para toda* contraseña, `ProfileValidator.validateNewPassword` la acepta si y solo si su longitud está entre 8 y 64 y contiene al menos una mayúscula, una minúscula y un dígito; en particular, cualquier contraseña de longitud mayor que 64 se rechaza (regla común añadida a `RegistrationValidator`, con paridad registro/perfil).

**Validates: Requirements 7.4, 15.2**

### Property 4: La unicidad excluye al propio usuario

*Para todo* conjunto de usuarios en `Local_Database` y todo usuario existente, actualizar sus datos personales conservando su mismo email y/o username normalizados nunca se considera conflicto; y usar un email/username que pertenece a un usuario con `id` distinto devuelve `EMAIL_TAKEN`/`USERNAME_TAKEN` respectivamente.

**Validates: Requirements 5.2, 5.3, 5.4**

### Property 5: El cambio de contraseña conserva la verificabilidad y nunca persiste texto plano

*Para toda* contraseña válida `p`, tras `changePassword`, `passwordHasher.verify(hash(p), p)` es verdadero, `verify(hash(p), q)` es falso para toda `q != p`, y ni el `Result` devuelto ni las columnas de credencial persistidas contienen `p` en texto plano (salt y hash Base64 son distintos de `p`).

**Validates: Requirements 7.11, 7.12**

### Property 6: El downscale produce una imagen dentro de 512×512 conservando proporción

*Para todo* par de dimensiones de origen `(w, h)` con `w>0, h>0`, la función pura de cálculo de dimensiones objetivo de `PhotoStorage` produce `(w', h')` con `w' <= 512` y `h' <= 512`, y la relación de aspecto se conserva dentro de una tolerancia (`|w/h - w'/h'|` acotada por el redondeo a enteros).

**Validates: Requirements 8.6**

### Property 7: La validación de formato/tamaño de imagen es correcta

*Para todo* par `(mimeType, sizeBytes)`, `PhotoStorage` acepta la imagen si y solo si `mimeType ∈ {image/jpeg, image/png, image/webp}` y `sizeBytes <= 10 MB`; en cualquier otro caso la rechaza sin modificar la foto actual.

**Validates: Requirements 8.7, 8.8**

### Property 8: Un evento de un solo consumo se entrega exactamente una vez

*Para toda* secuencia de llamadas a un `Event<T>`, `getContentIfNotHandled()` devuelve el contenido exactamente en la primera llamada y `null` en todas las siguientes, mientras que `peekContent()` nunca marca el evento como consumido.

**Validates: Requirements 14.1, 14.2, 14.3**

---

## Error Handling

Los errores de dominio viajan como `AuthError` dentro de `Result.Error` transportado por `AuthException` (mecanismo existente). La UI los extrae con `((AuthException) ex).getAuthError()` y los traduce a un recurso de string localizado. Cuando la excepción no es `AuthException`, se trata como error genérico (`PERSISTENCE_ERROR`).

### Tabla AuthError → recurso de string

| AuthError | Contexto | Recurso de string (ejemplo) |
|---|---|---|
| `USERNAME_TAKEN` | Guardar datos personales | `R.string.error_username_taken` |
| `EMAIL_TAKEN` | Guardar datos personales | `R.string.error_email_taken` |
| `INVALID_CREDENTIALS` | Contraseña actual incorrecta | `R.string.error_current_password_incorrect` |
| `PERSISTENCE_ERROR` | Fallo técnico de lectura/escritura/foto | `R.string.error_persistence` |
| `VALIDATION_ERROR` | Imagen no soportada / campos inválidos | `R.string.error_validation` |
| `SESSION_UNAVAILABLE` | Operar sin sesión válida | `R.string.error_session_unavailable` |
| `SESSION_CLEAR_FAILED` | Fallo al cerrar sesión | `R.string.error_session_clear_failed` |

Errores por campo de validación (`ValidationError`) se traducen igualmente a strings y se muestran con `TextInputLayout.setError` en el campo correspondiente (R3, R4, R7.2–R7.5). Ante error de guardado, la UI conserva los datos editados en el formulario para reintentar (R6.7, R13.4).

### Flujo de mapeo (UI)
```java
private String messageFor(Exception ex) {
    if (ex instanceof AuthException) {
        switch (((AuthException) ex).getAuthError()) {
            case USERNAME_TAKEN:       return getString(R.string.error_username_taken);
            case EMAIL_TAKEN:          return getString(R.string.error_email_taken);
            case INVALID_CREDENTIALS:  return getString(R.string.error_current_password_incorrect);
            case SESSION_UNAVAILABLE:  return getString(R.string.error_session_unavailable);
            case SESSION_CLEAR_FAILED: return getString(R.string.error_session_clear_failed);
            case VALIDATION_ERROR:     return getString(R.string.error_validation);
            case PERSISTENCE_ERROR:
            default:                   return getString(R.string.error_persistence);
        }
    }
    return getString(R.string.error_persistence);
}
```

Casos silenciosos (sin error al usuario): avatar con archivo inexistente → avatar por defecto (R1.7, R9.7); ranking sin métricas → placeholder/estado neutro (R11.5).

---

## Concurrency Strategy

- **`@IoExecutor` (existente):** todas las operaciones de perfil (carga, guardado, cambio de contraseña, foto) corren en el `ExecutorService` de `ExecutorModule` (`newFixedThreadPool(4)`), despachadas por los use cases igual que `LoginUseCase` (R13.7). El repositorio permanece síncrono devolviendo `Result`.
- **Post a main thread:** el `PerfilViewModel` publica resultados con `postValue` (heredado de `BaseViewModel`), seguro desde hilos de fondo. Los `Event<T>` se publican también con `postValue`.
- **Anti-doble-submit (R7.8, R13.1, R13.2):** antes de lanzar cada operación, el VM marca `isLoading=true` y expone un flag que deshabilita el control de acción; los envíos adicionales del mismo control se ignoran hasta éxito o error. El `PerfilFragment` deshabilita el botón mientras `isLoading` es verdadero.
- **Timeout de 30s (R13.5):** al lanzar la operación se programa un temporizador; si no hay respuesta en 30s, el VM oculta la carga, rehabilita el control y emite un `errorEvent` de timeout. La respuesta tardía posterior se ignora (idempotencia del estado por operación en curso).

---

## Testing Strategy

Enfoque dual: **pruebas unitarias/PBT en JVM** para la lógica pura y **tests de integración/instrumentación** para la infraestructura (Room, SharedPreferences, Bitmap real). Mapea a R16.

### Property-based tests (JVM, ≥100 iteraciones)
Librería PBT para JVM (jqwik o junit-quickcheck). Cada test se etiqueta con un comentario que referencia la propiedad del diseño:
`// Feature: profile-screen, Property N: <texto>`

- **Property 1** — normalización idempotente (helpers de normalización extraídos/visibles para test). 
- **Property 2** — `ProfileValidator.validateUsername` (delegando en `RegistrationValidator`).
- **Property 3** — `ProfileValidator.validateNewPassword` incluida la frontera de 64.
- **Property 4** — unicidad excluyendo al propio id, con `UserDao` in-memory (Room in-memory) o fake DAO.
- **Property 5** — `ChangePasswordUseCase`/`ProfileRepositoryImpl` con un `PasswordHasher` real o de test: verify(hash(p),p) y no exposición de texto plano (R16.3, R16.4).
- **Property 6** — función pura de cálculo de dimensiones de `PhotoStorage`.
- **Property 7** — validación de `(mimeType, sizeBytes)` de `PhotoStorage`.
- **Property 8** — estructura `Event<T>`.

### Tests unitarios JVM (ejemplos y bordes) — R16.1
- `ProfileValidator`/`RegistrationValidator` sobreescribiendo el hook `protected isEmailPattern(String)` para no depender de `android.util.Patterns` (subclase de test que devuelve un patrón regex simple). Casos: username formato-antes-que-longitud, email obligatorio/ formato/ vacío, password mín 8, máx 64, mayúscula/minúscula/dígito.
- Contador de intentos y bloqueo de 60s del `PerfilViewModel` (con reloj/scheduler inyectable o simulado); reinicio a cero al recrear el VM (R7.10).
- Timeout de 30s con executor/scheduler controlado (R13.5).
- Mapeo `AuthError` → string (con contexto de test).

### Tests de instrumentación (androidTest) — R16.2
- **Migración v4→v5** análogo a `MatchMigrationTest`: `MigrationTestHelper` crea la BD en v4 con el `CREATE TABLE users` de v4, siembra una fila de usuario, cierra, `runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)`, y verifica con `PRAGMA table_info(users)` que existe `avatar_uri`, que la fila se conserva sin cambios y que `avatar_uri` es NULL (R10.3/10.4/10.6).
- **`PhotoStorage`** con imágenes reales (si es factible en instrumentación): verifica que una imagen soportada se decodifica, corrige EXIF, produce ≤512×512, se escribe en `filesDir` con nombre determinista y que un formato/tamaño no soportado se rechaza (R8). Tests con 1–3 imágenes representativas (no PBT sobre bitmaps reales por coste).
- **Logout / `PreferencesManager.clearTokens()`** con instrumentación o fake: tras logout no queda ninguno de `access_token, refresh_token, user_id, user_name, is_logged_in, token_expiry` (R12.3).

### Por qué ciertas áreas NO usan PBT
- Migración Room, `clearTokens` y decodificación real de `Bitmap`/EXIF dependen de infraestructura Android; su comportamiento no varía significativamente con la entrada y 100 iteraciones no aportan valor frente al coste. Se cubren con integración/ejemplos representativos.

### Dependencias nuevas (build.gradle del módulo `app`)
- `androidx.exifinterface:exifinterface` — lectura/corrección de orientación EXIF.
- `androidx.core` (FileProvider) — ya suele estar presente vía `androidx.core:core`; confirmar disponibilidad de `FileProvider`.
- Librería de PBT de test: `net.jqwik:jqwik` (JUnit 5) o `com.pholser:junit-quickcheck-*` (JUnit 4), en `testImplementation`, acorde al runner de tests JVM existente del proyecto.

---

## Requirements Traceability

| Requisito | Componentes que lo satisfacen |
|---|---|
| **R1** Identificación y carga | `PerfilViewModel.load()` (isLoggedIn/getUserId), `LoadProfileUseCase`, `ProfileRepository.getCurrentUser`, `PerfilFragment` (loading, avatar por defecto, "null" evitado), `Event(LOGIN)` |
| **R2** Nombre completo solo lectura | `PerfilFragment` (control no editable), `ProfileFormState.fullName` desde `full_name`; `UpdatePersonalDataUseCase` no toca `full_name` |
| **R3** Edición de username | `ProfileValidator.validateUsername` → `RegistrationValidator` (formato antes que longitud); `PerfilFragment` errores por campo |
| **R4** Edición de email | `ProfileValidator.validateEmail` (hook `isEmailPattern`), normalización |
| **R5** Unicidad y normalización | `ProfileRepositoryImpl` (normalize/normalizeUsernameOrNull, `existsBy*ExcludingId`, `resolveConstraint`) |
| **R6** Persistencia datos personales | `UserDao.updatePersonalData`, `UpdatePersonalDataUseCase`, `ProfileRepositoryImpl`, `PerfilFragment` (Toast corto, conservar datos en error) |
| **R7** Cambio de contraseña | `ChangePasswordUseCase`, `ProfileRepositoryImpl.changePassword`, `PasswordHasher`, `UserDao.updateCredentials`, contador in-memory + bloqueo 60s en `PerfilViewModel`, `SESSION_UNAVAILABLE` |
| **R8** Selección de foto | `PhotoStorage` (validación/decode/EXIF/downscale), `FileProvider` (manifest + file_paths.xml), `PerfilFragment` (selector 2 opciones, permisos cámara) |
| **R9** Persistencia/visualización foto | Columna `avatar_uri`, `UserDao.updateAvatar`, `UpdateProfilePhotoUseCase`, `ProfileRepositoryImpl.updateAvatar`, `PhotoStorage` (nombre determinista, borrado del anterior), `UserEntity.toDomainModel` |
| **R10** Migración v4→v5 | `GolIADatabase` (version=5, `MIGRATION_4_5`), `DatabaseModule.addMigrations`, esquema JSON v5, test de migración |
| **R11** Historial | `RankingDataSource` + `PlaceholderRankingDataSource` (@Singleton, binding compartido con Inicio), `RankingSnapshot`, `PerfilFragment` (4 métricas) |
| **R12** Logout | `LogoutUseCase` → `PreferencesManager.clearTokens()`, `Event(LOGIN)` con `FLAG_ACTIVITY_NEW_TASK|CLEAR_TASK`, `SESSION_CLEAR_FAILED`, no borra el registro |
| **R13** Estados de UI | `BaseViewModel` (isLoading/errorMessage), anti-doble-submit, timeout 30s, mapeo `AuthError`→string, `@IoExecutor` |
| **R14** Estado y eventos single-use | `Event<T>`, `ProfileFormState`/preview retenidos en `PerfilViewModel` |
| **R15** Validación centralizada | `ProfileValidator` delega en `RegistrationValidator`; regla máx 64 añadida al validador compartido |
| **R16** Pruebas automatizadas | PBT (Prop 1–8), tests JVM de `ProfileValidator` (hook), test de migración v4→v5, test de `ChangePasswordUseCase` (verificación + re-hash + no texto plano), tests de `PhotoStorage` |
