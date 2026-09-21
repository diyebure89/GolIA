# Informe de Arquitectura — GolIA

Este documento explica cómo quedó organizada la app tras el refactor, por qué se
tomó cada decisión, y **cómo agregar una funcionalidad nueva** siguiendo el mismo
patrón. Está pensado para que puedas seguir construyendo sin volver a pelearte
con la estructura.

---

## 1. La idea en una frase

La app sigue **Clean Architecture + MVVM**: la UI no sabe de red ni de base de
datos, solo habla con un ViewModel; el ViewModel pide acciones a **casos de uso**;
los casos de uso trabajan contra **interfaces de repositorio** del dominio; y las
implementaciones concretas (Retrofit + Room) viven en la capa de datos. **Hilt**
conecta todo por inyección de dependencias.

La regla de oro (regla de dependencias): **las flechas apuntan siempre hacia el
dominio**. `presentation` y `data` dependen de `domain`; `domain` no depende de
nadie.

```
presentation (UI + ViewModels)
        │  usa
        ▼
domain (modelos, interfaces de repositorio, casos de uso, Result)
        ▲  implementa
        │
data (Retrofit, Room, mappers, repos concretos)
```

---

## 2. Estructura de paquetes

```
com.diyebure.golia
├── domain/                     ← Núcleo. No depende de Android salvo lo mínimo.
│   ├── common/                 Result<T>, Callback<T>  (vocabulario compartido)
│   ├── model/                  User, Match, Team, Competition, Prediction...
│   ├── repository/             AuthRepository, MatchRepository (INTERFACES)
│   └── usecase/                Casos de uso (una acción de negocio cada uno)
│       └── auth/               LoginUseCase, RegisterUseCase
│
├── data/                       ← Cómo se obtienen/guardan los datos.
│   ├── remote/                 Retrofit: api/, dto/, interceptor/
│   ├── local/                  Room: dao/, entity/, database/, PreferencesManager
│   ├── mapper/                 DTO/Entity ↔ modelo de dominio
│   └── repository/             AuthRepositoryImpl, MatchRepositoryImpl (IMPLES)
│
├── di/                         ← Cableado de Hilt (todo lo que "se inyecta").
│   ├── NetworkModule           Retrofit, OkHttp, interceptores, APIs
│   ├── DatabaseModule          GolIADatabase + los DAOs
│   ├── RepositoryModule        @Binds interfaz → implementación
│   ├── ExecutorModule          Pool de hilos de IO compartido
│   └── qualifier/IoExecutor    Anotación para pedir ese pool
│
├── ui/ (y raíz)                ← Presentación: Activities/Fragments + ViewModels.
│   └── auth/                   LoginViewModel, RegisterViewModel
│
└── GolIAApplication            @HiltAndroidApp (arranca el grafo de Hilt)
```

---

## 3. Decisiones tomadas y por qué

### 3.1. `Result<T>` se movió al dominio (`domain/common`)
Antes vivía en `data`, pero las interfaces del dominio lo usaban → el dominio
dependía de la capa de datos (violación de la regla de dependencias). Al moverlo
a `domain/common`, todas las capas comparten un mismo tipo de resultado sin
invertir las dependencias. `Result` modela "éxito con dato" o "error", y trae
utilidades (`isSuccess()`, `getOrNull()`, `map()`, `onSuccess/onError`).

### 3.2. Se creó `DatabaseModule` y se quitó el singleton de la base de datos
Antes `GolIADatabase` tenía un `getInstance()` estático (estado global oculto) y
**nadie proveía los DAOs**, así que `MatchRepositoryImpl` no se podía construir por
Hilt. Ahora `DatabaseModule` provee la base y cada DAO como `@Singleton`, y la
base ya incluye `PredictionEntity` + `predictionDao()` (antes faltaba). Ventaja:
en tests puedes cambiar la base real por una in-memory con una sola línea.

### 3.3. `RepositoryModule` usa `@Binds` (módulo abstracto)
Como `AuthRepositoryImpl` y `MatchRepositoryImpl` ya tienen constructor
`@Inject`, no hace falta construirlos a mano con `@Provides`. `@Binds` solo dice
"cuando alguien pida la interfaz, entrégale esta implementación". Es el patrón
idiomático y hace trivial cambiar de implementación (p. ej. un fake en tests).

### 3.4. Threading centralizado con `@IoExecutor`
Antes `MatchRepositoryImpl` creaba su propio `Executors.newFixedThreadPool(4)` y
varios métodos hacían llamadas de red **síncronas** (`.execute()`) que podían
correr en el hilo principal (riesgo de ANR / `NetworkOnMainThreadException`).
Ahora hay un único pool de IO compartido, provisto por `ExecutorModule` y pedido
con la anotación `@IoExecutor`. Los **casos de uso** ejecutan el trabajo bloqueante
en ese pool y devuelven el resultado por `Callback`, de modo que el ViewModel no
toca hilos y solo publica estado.

### 3.5. Autenticación real cableada por Hilt (se eliminó el código duplicado)
Había **dos** mecanismos de login: uno "bueno" (`AuthRepository` + `AuthRepositoryImpl`
con Retrofit) que nadie usaba, y uno legacy (`LoginRepository` + `LoginDataSource`
con un usuario falso "Jane Doe") que era el que la UI realmente invocaba. Se
eliminó el legacy y ahora:
- `LoginViewModel` / `RegisterViewModel` son `@HiltViewModel` y reciben los casos
  de uso por `@Inject`.
- `LoginActivity` / `RegistroActivity` llevan `@AndroidEntryPoint`.
- `LoginActivity` ahora **valida, llama al ViewModel y observa el estado**
  (`LOADING` deshabilita el botón, `SUCCESS` navega, `ERROR` muestra el mensaje),
  en vez de navegar directo sin autenticar.

### 3.6. `PreferencesManager` y `AuthInterceptor` por inyección
`PreferencesManager` pasó de `getInstance()` estático a constructor `@Inject`
(`@Singleton`), y `AuthInterceptor` ahora recibe el `PreferencesManager` inyectado
en vez de resolverlo con una llamada estática. Menos estado global, más testeable.

---

## 4. Flujo completo de una acción (ejemplo: login)

```
LoginActivity (@AndroidEntryPoint)
   │ 1. lee inputs y llama viewModel.login(email, pass)
   ▼
LoginViewModel (@HiltViewModel)
   │ 2. valida; publica LOADING; llama loginUseCase.execute(..., callback)
   ▼
LoginUseCase
   │ 3. corre en @IoExecutor (fuera del hilo principal)
   │    authRepository.login(email, pass)
   ▼
AuthRepositoryImpl (implementa AuthRepository)
   │ 4. AuthApiService (Retrofit) + guarda tokens en PreferencesManager
   │    mapea UserDto → User (dominio) y envuelve en Result<User>
   ▼
   callback.onResult(Result<User>)
   │ 5. el ViewModel hace postValue(SUCCESS/ERROR)
   ▼
LoginActivity observa loginState y navega o muestra el error
```

---

## 5. Cómo agregar una funcionalidad nueva (receta)

Ejemplo: mostrar el **ranking** de usuarios. Sigue el orden data → domain →
presentation, o de dominio hacia afuera. Recomendado:

1. **Dominio — modelo** (`domain/model/RankingEntry.java`): clase de datos pura,
   sin anotaciones de Android/Room/Retrofit.

2. **Dominio — interfaz de repositorio** (`domain/repository/RankingRepository.java`):
   define QUÉ se puede hacer, no CÓMO. Devuelve modelos de dominio y/o `Result`.
   ```java
   public interface RankingRepository {
       Result<List<RankingEntry>> getLeaderboard(String period);
   }
   ```

3. **Dominio — caso de uso** (`domain/usecase/ranking/GetLeaderboardUseCase.java`):
   una acción. Inyecta la interfaz + `@IoExecutor` y devuelve por `Callback`,
   igual que `LoginUseCase`.

4. **Datos — DTO + API** si viene de red (`data/remote/dto/`, `data/remote/api/`),
   y/o **Entity + DAO** si se cachea (`data/local/`). Añade el mapeo en
   `data/mapper/`.

5. **Datos — implementación del repositorio**
   (`data/repository/RankingRepositoryImpl.java`): `@Singleton`, constructor
   `@Inject` con la API/DAO que necesite; implementa la interfaz del dominio.

6. **DI — enlaza la interfaz**: añade en `RepositoryModule`:
   ```java
   @Binds @Singleton
   public abstract RankingRepository bindRankingRepository(RankingRepositoryImpl impl);
   ```
   Si agregaste un DAO nuevo, provéelo en `DatabaseModule` y añade la entity al
   `@Database` de `GolIADatabase`.

7. **Presentación — ViewModel** (`ui/ranking/RankingViewModel.java`): `@HiltViewModel`,
   inyecta el caso de uso, expone estado con `LiveData` (una lista + un estado de
   carga + un mensaje de error). Usa `postValue` en el callback.

8. **Presentación — Activity/Fragment**: `@AndroidEntryPoint`, obtiene el
   ViewModel con `new ViewModelProvider(this).get(...)`, observa el `LiveData` y
   pinta la UI. Nada de red ni Room aquí.

Regla mental: **si una clase de UI importa algo de `data/`, algo está mal.** La UI
solo debe conocer su ViewModel y los modelos de `domain/`.

---

## 6. Convenciones rápidas

- **Nombres**: interfaz `XxxRepository` en `domain/repository`, implementación
  `XxxRepositoryImpl` en `data/repository`. Caso de uso: `VerboSustantivoUseCase`.
- **Threading**: cualquier trabajo bloqueante (red, disco, Room) va en el caso de
  uso sobre `@IoExecutor`. El ViewModel nunca bloquea el hilo principal.
- **Estado de UI**: exponer `LiveData` inmutable desde el ViewModel
  (`LiveData` público, `MutableLiveData` privado). Publicar con `postValue` cuando
  vienes de background, `setValue` desde el hilo principal.
- **Errores**: propágalos dentro de `Result.Error`; el ViewModel traduce a un
  mensaje para el usuario.
- **DI**: preferir constructor `@Inject` + `@Binds`. Usar `@Provides` solo cuando
  no controlas el constructor (Retrofit, Room, `SharedPreferences`).

---

## 7. Deuda técnica / próximos pasos sugeridos

Cosas que quedaron fuera del alcance de este refactor y conviene abordar luego:

1. **Conectar los Fragments a datos reales.** Hoy `PartidosFragment` y compañía
   usan datos hardcodeados. Falta crear sus ViewModels (`@HiltViewModel`) que usen
   `MatchRepository`/casos de uso, y anotar los Fragments con `@AndroidEntryPoint`.
2. **Interfaz `AuthRepository` demasiado ancha.** Expone `saveAuthTokens()` y
   `clearSession()`, que son detalle de implementación. Podrían salir de la
   interfaz pública del dominio.
3. **`SessionManager`/estado de sesión global** para decidir en el arranque si ir a
   Login o a Home (hoy la navegación post-login es directa).
4. **Consolidar el árbol de presentación.** Existe `presentation/ui/*` (con clases
   base `BaseActivity`/`BaseFragment`/`BaseViewModel` y ViewBinding) prácticamente
   vacío, en paralelo a la UI real en la raíz + `ui/auth`. Conviene elegir uno solo
   y migrar las pantallas para reutilizar las bases y ViewBinding.
5. **Migraciones de Room.** Hoy usa `fallbackToDestructiveMigration()` (borra datos
   al cambiar el esquema). Antes de producción, escribir `Migration` reales.
6. **Tests.** Con la DI por interfaces ya es fácil: inyecta fakes de los
   repositorios y un executor síncrono para probar ViewModels y casos de uso.

---

## 8. Verificación

Los cambios se revisaron estáticamente (imports, firmas de interfaz vs.
implementación, ausencia de referencias a clases eliminadas). **La compilación
completa debe correrse en tu máquina** con:

```
gradlew assembleDebug
```

El procesamiento de anotaciones de Hilt (que genera el grafo de inyección) no se
pudo ejecutar hasta el final en el entorno donde se hizo el refactor, así que esa
validación final queda de tu lado.
