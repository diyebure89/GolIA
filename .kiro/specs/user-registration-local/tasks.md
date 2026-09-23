# Implementation Plan: Registro e Inicio de Sesión Local (`user-registration-local`)

## Overview

Este plan convierte el diseño aprobado en una serie de tareas de codificación incrementales sobre la base de código Java existente (Clean Architecture + MVVM + Hilt + Room, paquete `com.diyebure.golia`). El orden respeta la regla de dependencias: primero el dominio y los datos (modelos, seguridad, validación, entity/DAO/mapper), luego la implementación local del repositorio, los use cases y la base de datos, después el cableado de DI, a continuación los ViewModels y por último la UI y los recursos. Las tareas de test se colocan cerca de la implementación que verifican y cierran el plan con la integración.

El lenguaje de implementación es **Java** (definido en el diseño), reutilizando `Result`, `Callback`, `@IoExecutor` y los patrones de entity/DAO/módulos Hilt ya presentes.

Las tareas marcadas con `*` son opcionales (tests) y pueden omitirse para un MVP más rápido, pero se recomienda ejecutarlas para cubrir las Correctness Properties del diseño.

## Tasks

- [x] 1. Modelo de dominio, contrato de errores y objetos de seguridad
  - [x] 1.1 Añadir `fullName` al modelo de dominio `User`
    - Modificar `domain/model/User.java`: añadir campo `fullName` como primer atributo tras `id`, actualizar el constructor completo y añadir el getter `getFullName()`.
    - Mantener el resto de atributos y getters sin cambios (el modelo NO contiene credenciales).
    - _Requirements: 8.2_

  - [x] 1.2 Crear el contrato tipado de errores de autenticación
    - Crear `domain/error/AuthError.java` (enum) con `USERNAME_TAKEN`, `EMAIL_TAKEN`, `INVALID_CREDENTIALS`, `PERSISTENCE_ERROR`, `VALIDATION_ERROR`.
    - Crear `domain/error/AuthException.java` que extiende `Exception`, porta un `AuthError` (constructores con y sin `cause`) y expone `getAuthError()`; para viajar dentro de `Result.Error` sin cambiar la firma de `Result`.
    - _Requirements: 15.1_

  - [x] 1.3 Crear los objetos de seguridad del dominio
    - Crear `domain/security/PasswordCredential.java`: objeto de valor inmutable con `algorithm`, `iterations`, `saltBase64`, `hashBase64` y sus getters.
    - Crear `domain/security/PasswordHasher.java` (interfaz): `PasswordCredential hash(String plainPassword)` y `boolean verify(PasswordCredential credential, String plainPassword)`.
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 2. Validación centralizada del dominio
  - [x] 2.1 Crear los tipos de validación
    - Crear `domain/validation/ValidationError.java` (enum) con todos los errores por campo del diseño (`FULL_NAME_REQUIRED`, `FULL_NAME_TOO_SHORT`, `FULL_NAME_TOO_LONG`, `USERNAME_TOO_SHORT`, `USERNAME_TOO_LONG`, `USERNAME_FORMAT`, `EMAIL_REQUIRED`, `EMAIL_INVALID`, `PASSWORD_REQUIRED`, `PASSWORD_TOO_SHORT`, `PASSWORD_NO_UPPER`, `PASSWORD_NO_LOWER`, `PASSWORD_NO_DIGIT`, `CONFIRM_REQUIRED`, `CONFIRM_MISMATCH`).
    - Crear `domain/validation/ValidationResult.java`: enum interno `Field` (`FULL_NAME`, `USERNAME`, `EMAIL`, `PASSWORD`, `CONFIRM_PASSWORD`), mapa `Field -> ValidationError`, `isValid()`, `errorFor(Field)` y `getErrors()`.
    - _Requirements: 14.1, 15.1_

  - [x] 2.2 Implementar `RegistrationValidator` con las reglas exactas
    - Crear `domain/validation/RegistrationValidator.java` con constructor `@Inject`.
    - `validateFullName`: tras `trim`, obligatorio; longitud en `[3, 50]`.
    - `validateUsername`: `trim`, retirar `@` inicial; si queda vacío ⇒ válido (no proporcionado); si no, debe cumplir `^[A-Za-z0-9_.]{3,30}$`, devolviendo error de longitud o de formato según corresponda.
    - `validateEmail`: obligatorio; delegar el patrón en un método `protected boolean isEmailPattern(String)` que en producción usa `android.util.Patterns.EMAIL_ADDRESS` (sobreescribible en test para JVM pura).
    - `validatePassword`: obligatorio; `>= 8`; al menos una mayúscula, una minúscula y un dígito, devolviendo el error de la primera condición incumplida.
    - `validateConfirm`: obligatorio; igual carácter por carácter a `password`.
    - `validateRegistration(...)`: compone los cinco campos en un `ValidationResult`.
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 14.1, 14.2_

  - [ ]* 2.3 Escribir property test del nombre completo
    - **Property 1: Validación del nombre completo por longitud**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
    - Usar jqwik (`@Property(tries = 100)`), sobreescribir `isEmailPattern` para JVM pura; generadores de blancos y longitudes de frontera.

  - [ ]* 2.4 Escribir property test del nombre de usuario opcional
    - **Property 2: Validación del nombre de usuario opcional**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
    - Generadores con `@` inicial, espacios internos y caracteres fuera de `[A-Za-z0-9_.]`.

  - [ ]* 2.5 Escribir property test de la contraseña
    - **Property 3: Validación de la contraseña por complejidad**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6**

  - [ ]* 2.6 Escribir property test de la confirmación de contraseña
    - **Property 4: Validación de la confirmación de contraseña**
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [ ]* 2.7 Escribir tests de ejemplo del patrón de email
    - Casos representativos válidos/inválidos para `isEmailPattern` (evitar re-implementar el patrón de Android).
    - _Requirements: 3.2, 3.3_

- [x] 3. Checkpoint - Validación de dominio
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Hasher PBKDF2 en la capa de datos
  - [x] 4.1 Implementar `Pbkdf2PasswordHasher`
    - Crear `data/security/Pbkdf2PasswordHasher.java` (`@Singleton`, constructor `@Inject`) implementando `PasswordHasher`.
    - `hash`: `PBKDF2WithHmacSHA256`, salt de 16 bytes con `SecureRandom`, 120000 iteraciones, clave de 256 bits; devolver `PasswordCredential` con salt/hash en Base64.
    - `verify`: recalcular con algoritmo/iteraciones/salt de la credencial y comparar con `MessageDigest.isEqual` (tiempo constante); derivar la longitud de clave de los bytes del hash almacenado.
    - Aislar Base64 tras un pequeño helper para poder testear en JVM (produccion `android.util.Base64` NO_WRAP).
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [ ]* 4.2 Escribir test de ejemplo de forma de la credencial
    - **Property 5: Contenido y seguridad de la credencial generada**
    - **Validates: Requirements 6.1, 6.2**
    - Comprobar iteraciones = 120000, salt decodifica a 16 bytes, hash a 32 bytes y que ningún campo contiene la contraseña en texto plano.

  - [ ]* 4.3 Escribir property test de round-trip positivo
    - **Property 6: Round-trip de verificación de contraseña (positivo)**
    - **Validates: Requirements 6.3, 6.4, 6.5**
    - `verify(hash(p), p)` es verdadero para toda contraseña `p`.

  - [ ]* 4.4 Escribir property test de round-trip negativo
    - **Property 7: Rechazo de contraseña incorrecta (negativo)**
    - **Validates: Requirements 6.6**
    - `verify(hash(p), p')` es falso para todo par `p != p'`.

- [x] 5. Entity, DAO y mapper de usuario
  - [x] 5.1 Crear `UserEntity`
    - Crear `data/local/entity/UserEntity.java` con `@Entity(tableName = "users")` e `indices` UNIQUE en `email` y `username`.
    - Columnas: `id` (`@PrimaryKey @NonNull`, UUID), `full_name`, `username` (nullable), `email` (`@NonNull`), `password_algorithm`, `password_iterations` (int), `password_salt`, `password_hash`, `created_at` (long epoch UTC).
    - Constructor vacío + completo, factoría `newUser(...)` a partir de `PasswordCredential`, `toCredential()` y `toDomainModel()` (sin exponer credenciales; `country`/`avatarUrl`/estadísticas por defecto).
    - _Requirements: 7.4, 8.1, 8.2, 8.3_

  - [x] 5.2 Crear `UserDao`
    - Crear `data/local/dao/UserDao.java` (`@Dao`): `insert` con `OnConflictStrategy.ABORT`, `findByEmail`, `findByUsername`, `findByUsernameOrEmail`, `existsByEmail`, `existsByUsername`, `getById`.
    - _Requirements: 7.2, 7.3, 7.4, 7.5, 9.2_

  - [x] 5.3 Crear `UserEntityMapper`
    - Crear `data/mapper/UserEntityMapper.java` con `toDomain(UserEntity)` (delegando en `toDomainModel()`); sin `toEntity` con credencial pública (la entidad se crea vía `UserEntity.newUser` dentro del repositorio).
    - _Requirements: 8.6_

  - [ ]* 5.4 Escribir unit tests del `UserEntity`/mapper
    - Verificar `newUser`/`toCredential`/`toDomainModel` (round-trip de credencial y que el dominio no expone hash).
    - _Requirements: 8.2, 8.6_

- [x] 6. Contrato del repositorio y use cases
  - [x] 6.1 Ajustar las firmas de `AuthRepository`
    - Modificar `domain/repository/AuthRepository.java`: `Result<User> login(String identifier, String password)` y `Result<User> register(String fullName, String username, String email, String password)`; conservar `logout`, `refreshToken`, `isLoggedIn`, `getCurrentUser`, `saveAuthTokens`, `clearSession`.
    - _Requirements: 8.6, 9.1_

  - [x] 6.2 Ajustar `RegisterUseCase` y `LoginUseCase`
    - Modificar `domain/usecase/auth/RegisterUseCase.java`: `execute(fullName, username, email, password, Callback<User>)` ejecutando en `@IoExecutor` y devolviendo por `Callback`.
    - Modificar `domain/usecase/auth/LoginUseCase.java`: `execute(identifier, password, Callback<User>)` con el mismo patrón.
    - _Requirements: 7.1, 8.4, 9.1_

- [x] 7. Implementación local del repositorio
  - [x] 7.1 Implementar `LocalAuthRepositoryImpl`
    - Crear `data/repository/LocalAuthRepositoryImpl.java` (`@Singleton`, `@Inject` de `UserDao` y `PasswordHasher`).
    - `normalize`: `trim + toLowerCase(Locale.ROOT)`; `normalizeUsernameOrNull`: `trim`, retirar `@` inicial, `toLowerCase`, `""` ⇒ `null`.
    - `register`: normalizar; chequeo previo `existsByUsername`/`existsByEmail` (`USERNAME_TAKEN`/`EMAIL_TAKEN`); `hash`; `insert(UserEntity.newUser(UUID, ...))`; capturar `SQLiteConstraintException` como salvaguarda re-discriminando el campo; capturar genérico como `PERSISTENCE_ERROR`; devolver `Result.Success(User con id)` sin datos parciales.
    - `login`: normalizar identificador; `findByUsernameOrEmail`; si null o `verify` falso ⇒ `INVALID_CREDENTIALS`; si coincide ⇒ `Result.Success`.
    - Implementación mínima local de `logout`/`refreshToken`/`getCurrentUser`/`isLoggedIn`/`saveAuthTokens`/`clearSession`.
    - _Requirements: 6.3, 6.5, 6.6, 7.1, 7.2, 7.3, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 9.1, 9.2, 9.3, 9.4, 9.5_

  - [ ]* 7.2 Escribir property test de normalización
    - **Property 8: Normalización idempotente y case-insensitive**
    - **Validates: Requirements 7.1, 9.1**

  - [ ]* 7.3 Escribir property test de unicidad de email (Room in-memory)
    - **Property 9: Unicidad case-insensitive de email**
    - **Validates: Requirements 7.3, 7.5**
    - `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries()` con `UserDao` real y hasher determinista.

  - [ ]* 7.4 Escribir property test de unicidad de username y coexistencia de NULL
    - **Property 10: Unicidad case-insensitive de username no nulo y coexistencia de NULL**
    - **Validates: Requirements 7.2, 7.4, 8.3, 2.1**

  - [ ]* 7.5 Escribir property test de round-trip de persistencia
    - **Property 11: Round-trip de persistencia del registro**
    - **Validates: Requirements 8.1, 8.2, 8.4**

  - [ ]* 7.6 Escribir property test de round-trip registro → login
    - **Property 12: Round-trip registro → login**
    - **Validates: Requirements 9.2, 9.5**

  - [ ]* 7.7 Escribir property test de credenciales inválidas
    - **Property 13: Credenciales inválidas en login**
    - **Validates: Requirements 9.3, 9.4**

  - [ ]* 7.8 Escribir test edge de persistencia parcial en error
    - `UserDao` mock cuyo `insert` lanza excepción ⇒ `Result.Error(PERSISTENCE_ERROR)`; con Room real, verificar que el conteo de filas no cambia.
    - _Requirements: 8.5_

- [x] 8. Adaptar la implementación remota y el mapper existente
  - [x] 8.1 Adaptar `AuthRepositoryImpl` (remoto, inactivo) a las nuevas firmas
    - Modificar `data/repository/AuthRepositoryImpl.java` para implementar `login(identifier, password)` y `register(fullName, username, email, password)`; queda como implementación remota no bindeada (compila pero no se usa en el MVP).
    - _Requirements: 8.6_

  - [x] 8.2 Mapear `fullName` en `UserMapper`
    - Modificar `data/mapper/UserMapper.java`: pasar `fullName` en `toDomain`/`toDto` (el DTO remoto puede pasar `null`/vacío mientras la API no tenga el campo).
    - _Requirements: 8.2_

- [x] 9. Base de datos y cableado de inyección de dependencias
  - [x] 9.1 Actualizar `GolIADatabase`
    - Modificar `data/local/database/GolIADatabase.java`: añadir `UserEntity` a `entities`, declarar `public abstract UserDao userDao()` y subir `version` de 1 a 2 (usa `fallbackToDestructiveMigration()`).
    - _Requirements: 8.1, 8.2_

  - [x] 9.2 Proveer `UserDao` en `DatabaseModule`
    - Modificar `di/DatabaseModule.java`: `@Provides @Singleton UserDao provideUserDao(GolIADatabase database)`.
    - _Requirements: 8.6_

  - [x] 9.3 Bindear `AuthRepository` a la implementación local
    - Modificar `di/RepositoryModule.java`: `@Binds @Singleton` de `AuthRepository` a `LocalAuthRepositoryImpl` (el remoto deja de estar bindeado).
    - _Requirements: 8.6_

  - [x] 9.4 Crear `SecurityModule`
    - Crear `di/SecurityModule.java` (`@Module`, `@InstallIn(SingletonComponent.class)`): `@Binds @Singleton` de `PasswordHasher` a `Pbkdf2PasswordHasher`. `RegistrationValidator` no necesita binding (clase concreta con `@Inject`).
    - _Requirements: 6.1, 14.1_

- [x] 10. Checkpoint - Dominio, datos y DI integrados
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Wrapper de eventos y ViewModels
  - [x] 11.1 Crear `Event<T>`
    - Crear `ui/common/Event.java`: `getContentIfNotHandled()` (contenido solo la primera vez, `null` después) y `peekContent()`.
    - _Requirements: 13.1, 13.2_

  - [ ]* 11.2 Escribir property test de consumo único de eventos
    - **Property 14: Consumo único de eventos**
    - **Validates: Requirements 13.1, 13.2**

  - [x] 11.3 Implementar `RegisterViewModel`
    - Modificar `ui/auth/RegisterViewModel.java` (`@HiltViewModel`): recibir los cinco campos incluido `fullName`; validar vía `RegistrationValidator` y publicar errores por campo como `Event<Map<Field, ValidationError>>`; invocar el `RegisterUseCase` real; estado `LOADING` como `LiveData` normal (bloquea doble submit); emitir eventos de éxito/error one-shot; mapear `AuthError` a string; NO tocar `PreferencesManager`.
    - _Requirements: 10.1, 10.2, 10.3, 11.1, 13.1, 13.2, 13.3, 14.1, 15.2_

  - [x] 11.4 Implementar `LoginViewModel`
    - Modificar `ui/auth/LoginViewModel.java` (`@HiltViewModel`): recibir identificador (username o email) + password; validación mínima de no vacíos; `LOADING` que bloquea doble submit; invocar `LoginUseCase`; en éxito guardar sesión con `PreferencesManager.setLoggedIn(true)` + `saveUserId(id)` antes de emitir navegación a `Home_Screen`; en error emitir `INVALID_CREDENTIALS` como evento para Toast global; eventos one-shot; mapear `AuthError` a string.
    - _Requirements: 9.1, 10.4, 10.5, 11.2, 13.1, 13.2, 13.3, 15.2_

  - [ ]* 11.5 Escribir tests de ejemplo de doble submit
    - Con executor síncrono/directo y use case falso: invocar `register`/`login` dos veces durante `LOADING` y verificar una sola ejecución.
    - _Requirements: 10.1, 10.4_

  - [ ]* 11.6 Escribir test de ejemplo de sesión (no auto-login en registro)
    - Con `PreferencesManager` mock: tras `register` exitoso `setLoggedIn` NO se invoca; tras `login` exitoso SÍ (`setLoggedIn(true)` + `saveUserId`).
    - _Requirements: 11.1, 11.2_

  - [ ]* 11.7 Escribir test de ejemplo del mapeo `AuthError` → string
    - Test exhaustivo por valor del enum ⇒ recurso distinto y no nulo.
    - _Requirements: 15.2_

- [x] 12. Recursos de UI
  - [x] 12.1 Añadir strings de validación y errores tipados
    - Modificar `res/values/strings.xml`: mensajes por `ValidationError` (p.ej. `fullname_too_short`), errores por `AuthError` (`error_username_taken`, `error_email_taken`, `error_invalid_credentials`, `error_persistence`, `error_validation`, `error_generic`), `registration_success`, y etiquetas con asterisco / "(opcional)" / leyenda "* Campos obligatorios".
    - _Requirements: 12.1, 12.2, 12.3, 15.2_

  - [x] 12.2 Añadir color de error
    - Modificar `res/values/colors.xml`: `<color name="error_red">#FF6B6B</color>`.
    - _Requirements: 12.4_

  - [x] 12.3 Marcar campos obligatorios/opcional en `activity_registro.xml`
    - Modificar el layout para mostrar asterisco rojo en Nombre, Correo, Contraseña y Confirmar (vía `HtmlCompat.fromHtml`/`SpannableString` sobre las etiquetas), "(opcional)" en Usuario y una leyenda "* Campos obligatorios" en `error_red`.
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

- [x] 13. Cableado de Activities
  - [x] 13.1 Cablear `RegistroActivity`
    - Modificar `RegistroActivity.java`: cablear `editText_nombre`/`textInputLayout_nombre`; al pulsar `button_registro` pasar los 5 valores al `RegisterViewModel`; observar errores por campo y hacer `setError(...)` en cada `TextInputLayout` con el string mapeado; mostrar/ocultar `progressBar_registro` y deshabilitar/habilitar `button_registro`; en éxito Toast `registration_success` y navegar a `LoginActivity` (sin iniciar sesión); `textView_iniciar_sesion` navega a `LoginActivity`; `button2` regresa (`onBackPressedDispatcher`/`finish()`); observar vía `Event<T>`.
    - _Requirements: 10.1, 10.2, 10.3, 11.1, 11.3, 11.4, 12.1, 12.2, 12.3, 13.2, 15.2_

  - [x] 13.2 Cablear `LoginActivity`
    - Modificar `LoginActivity.java`: usar `input_correo` como identificador; llamar `LoginViewModel.login(identifier, password)`; mostrar/ocultar carga y deshabilitar el botón durante `LOADING`; en éxito navegar a `Home_Screen`; en error `INVALID_CREDENTIALS` mostrar Toast global (no `setError` por campo); observar vía `Event<T>`.
    - _Requirements: 10.4, 10.5, 11.2, 13.2, 15.2_

  - [x] 13.3 Añadir placeholder de "¿Olvidaste la contraseña?" en `LoginActivity`
    - Añadir en `res/values/strings.xml` el string `recuperar_password_no_disponible` = "La recuperación de contraseña no está disponible en esta versión".
    - En `LoginActivity.java`: enlazar el `TextView` `textView6` y registrar un `OnClickListener` que muestre un Toast con ese string. No hay ViewModel/use case/repositorio: es puramente de UI (placeholder). No ejecutar ningún flujo de recuperación.
    - _Requirements: 16.1, 16.2, 16.3_

- [x] 14. Checkpoint final - Verificación completa
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Las tareas marcadas con `*` son opcionales (tests unitarios, de ejemplo, edge y basados en propiedades) y pueden omitirse para un MVP más rápido.
- Cada tarea referencia los sub-requisitos concretos que satisface para trazabilidad.
- Los tests basados en propiedades usan jqwik (`testImplementation` en `app/build.gradle`, mínimo 100 iteraciones por propiedad) y cada uno se etiqueta con `// Feature: user-registration-local, Property {n}: {texto}`.
- Las propiedades del repositorio/persistencia se prueban con Room in-memory y `UserDao` real; los fallos de escritura se simulan con `UserDao` mock.
- Los checkpoints aseguran validación incremental en cada corte importante.
- La verificación visual de layouts, contraste y Toast/progress (R10.2/10.3/10.5, R11.2–11.4, R12) es instrumentada/manual y queda fuera de las tareas de codificación automatizables.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "2.1"] },
    { "id": 1, "tasks": ["2.2", "4.1", "5.1", "6.1"] },
    { "id": 2, "tasks": ["2.3", "2.4", "2.5", "2.6", "2.7", "4.2", "4.3", "4.4", "5.2", "5.3", "6.2", "8.2", "11.1"] },
    { "id": 3, "tasks": ["5.4", "7.1", "8.1", "9.1", "11.2", "12.1", "12.2"] },
    { "id": 4, "tasks": ["7.2", "7.3", "7.4", "7.5", "7.6", "7.7", "7.8", "9.2", "9.3", "9.4", "11.3", "11.4", "12.3"] },
    { "id": 5, "tasks": ["11.5", "11.6", "11.7", "13.1", "13.2", "13.3"] }
  ]
}
```
