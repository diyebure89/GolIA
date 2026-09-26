# Implementation Plan: football-news-feed

## Overview

Este plan convierte el diseño de `football-news-feed` en pasos de codificación incrementales para la app Android/Java (Clean Architecture + MVVM + Hilt + Room + Retrofit + OkHttp). Cada tarea construye sobre las anteriores y termina cableando la pantalla real de noticias al `NoticiasFragment` y a la navegación existente.

El orden sigue el flujo: spike de contrato → configuración → capa de red → dominio → capa de datos (util, remota, Room) → repositorio → presentación → cableado final. El spike se marca temprano porque puede refinar constantes (fechas, campos obligatorios, coste por crédito, error 429); el resto puede avanzar en paralelo con los valores por defecto documentados en el diseño.

El lenguaje de implementación es **Java**, tal como usa el proyecto existente y el diseño (no hubo pseudocódigo, por lo que no se solicita elección de lenguaje).

Las subtareas marcadas con `*` (pruebas unitarias, de propiedades e integración) son opcionales y no se implementan automáticamente durante la ejecución.

## Tasks

- [x] 1. Spike: validar el contrato real de NewsData.io (tarea temprana)
  - Realizar una llamada real al endpoint `/news` del plan gratuito y registrar los hallazgos en un breve documento/nota de código (comentario o `NOTES` en el spec).
  - Confirmar: contrato del DTO de `/news` (nombres y nulabilidad de `title`, `description`, `link`, `image_url`, `source_id`/`source_name`, `pubDate`); formato/zona de `pubDate`; consumo de crédito y paginación (`page`/`nextPage`, tamaño de página, cabeceras de rate limit); forma del error 429 (código HTTP y cuerpo); compatibilidad de `q` + `language=es` + `category=sports`.
  - Anotar los ajustes que impactan constantes/decisiones aguas abajo: patrón de fecha del `NewsMapper`, conjunto de campos obligatorios, coste por refresco del `NewsRequestBudgetManager` y mapeo del error de cuota.
  - _Requirements: 3.4, 3.7, 3.11, 6.4, 6.10_

- [x] 2. Configuración de build y API key
  - Añadir en `app/build.gradle` la lectura de `NEWSDATA_API_KEY` desde `local.properties` y exponerla con `buildConfigField "String", "NEWSDATA_API_KEY", ...`, replicando el patrón de `API_FOOTBALL_KEY`.
  - Asegurar que `local.properties` está en `.gitignore` y que el valor literal no queda versionado. Confirmar que Glide ya está declarado (no se añaden dependencias nuevas).
  - _Requirements: 5.1, 5.2_

- [x] 3. Capa de red: cliente NewsData.io
  - [x] 3.1 Crear `NewsDataApiService` (Retrofit) con `@GET("news")` y parámetros `q`, `language`, `category`
    - Definir la interfaz devolviendo `Call<NewsResponseDto>`; no exponer `apikey` como argumento de método.
    - _Requirements: 3.4, 5.4_

  - [x] 3.2 Crear `NewsApiKeyInterceptor` que adjunta `apikey` como query param desde `BuildConfig.NEWSDATA_API_KEY`
    - Replica `ApiKeyInterceptor` pero añade la clave a la URL con `addQueryParameter("apikey", ...)`.
    - _Requirements: 5.3, 5.4_

  - [x] 3.3 Ampliar `NetworkModule` con el stack DI `@Named("newsdata")`
    - Proveer `OkHttpClient` propio (con `NewsApiKeyInterceptor` + logging), `Retrofit` con `baseUrl = https://newsdata.io/api/1/` y `provideNewsDataApiService`, aislado de los backends existentes.
    - _Requirements: 5.3, 4.1_

  - [ ]* 3.4 Escribir prueba unitaria del `NewsApiKeyInterceptor`
    - Verificar que toda petición adjunta el query param `apikey` sin pasarla por método.
    - _Requirements: 5.3, 5.4_

- [x] 4. DTOs del proveedor y mapper a dominio
  - [x] 4.1 Crear DTOs `NewsResponseDto` y `NewsArticleDto` en `data/remote/dto`
    - Campos según el contrato (ajustar nombres a lo confirmado por el spike); referenciados solo en capas remote/mapper.
    - _Requirements: 4.3_

  - [x] 4.2 Implementar `NewsMapper` (DTO → `ArticuloNoticia`)
    - Conservar título, descripción, imageUrl, sourceName, articleUrl y fecha; descartar artículos sin título o sin URL; normalizar `pubDate` a epoch UTC y descartar los no parseables; promover `http`→`https` en URLs.
    - _Requirements: 3.7, 3.8, 3.11, 4.5, 13.3_

  - [ ]* 4.3 Escribir prueba de propiedades del `NewsMapper` (validez + fecha)
    - **Property 6: El mapper solo emite artículos válidos con fecha parseable**
    - **Validates: Requirements 3.7, 3.8, 3.11, 4.5**

  - [ ]* 4.4 Escribir prueba de propiedades de orden de salida del mapper/repositorio
    - **Property 7: Orden por fecha de publicación descendente**
    - **Validates: Requirements 3.10**

  - [ ]* 4.5 Escribir pruebas unitarias del `NewsMapper` con ejemplos de `pubDate`
    - Ejemplos del formato de fecha confirmado por el spike y de descarte por campos ausentes.
    - _Requirements: 3.7, 3.11, 4.5_

- [x] 5. Dominio: modelo, contrato, consulta de ligas y errores
  - [x] 5.1 Crear el modelo de dominio `ArticuloNoticia`
    - Campos: articleId, title, description, imageUrl, sourceName, articleUrl, publishedAtEpochUtc.
    - _Requirements: 3.7, 4.4_

  - [x] 5.2 Definir la interfaz `NewsRepository` y los errores tipados `NewsError`
    - Exponer solo tipos de dominio (`getCachedNews`, `refreshNews` → `Result<List<ArticuloNoticia>>`); definir subclases `NetworkError`, `ProviderError`, `QuotaExceededError`, `PersistenceError`, `TimeoutError`.
    - _Requirements: 4.4, 11.5_

  - [x] 5.3 Implementar `LeagueNewsQuery` con la tabla inicial y la clave `wc_qualification`
    - Mapa inmutable leagueId→términos priorizados con la tabla del diseño; `WC_QUALIFICATION_IDS = {32,34,29,30,31,33,37}`; API `termsFor`, `aggregatedTerms`, `hasTermsFor`.
    - _Requirements: 1.4, 1.6, 3.1, 3.3_

  - [ ]* 5.4 Escribir prueba de propiedades de cobertura de términos
    - **Property 2: Cobertura de términos de LeagueNewsQuery** (itera `FixtureMapper.TARGET_LEAGUE_IDS`)
    - **Validates: Requirements 1.6**

  - [x] 5.5 Implementar `GetNewsUseCase` y `RefreshNewsUseCase`
    - Consumen `NewsRepository`, devuelven `Result<...>` tipado y ejecutan I/O en `@IoExecutor`.
    - _Requirements: 4.1, 6.1, 6.2, 11.4, 11.5_

- [x] 6. Utilidades: URL canónica y gestor de presupuesto
  - [x] 6.1 Implementar `util/CanonicalUrl` (normalización + SHA-256)
    - Normalizar esquema/host, promover a https, eliminar parámetros de tracking y fragmento, reordenar query params; `articleId = SHA-256(canonicalUrl)`.
    - _Requirements: 13.3_

  - [ ]* 6.2 Escribir prueba de propiedades de la URL canónica
    - **Property 8: Estabilidad de la URL canónica frente a parámetros de tracking**
    - **Validates: Requirements 6.6, 6.11**

  - [x] 6.3 Implementar `NewsRequestBudgetManager` (`@Singleton`)
    - Reset 00:00 UTC, límite operativo 180, intervalo mínimo 30 min (1800 s) por (chip, consulta), `decide(...)` como sección crítica `synchronized`, flag de cuota agotada por 429, reloj inyectable (`currentTimeMillis`/`today` overridables); contador diario en `PreferencesManager`.
    - _Requirements: 6.3, 6.4, 6.5, 6.9, 6.10_

  - [ ]* 6.4 Escribir pruebas dirigidas del `NewsRequestBudgetManager` con reloj inyectable
    - No exceder 180; reset al cruzar 00:00 UTC; descarte de refresco antes de 1800 s por liga; 429 fuerza solo-caché; test de concurrencia donde solo un hilo obtiene `PROCEED`.
    - _Requirements: 6.3, 6.4, 6.5, 6.9, 6.10_

- [x] 7. Capa de datos remota: builder de consulta
  - [x] 7.1 Implementar `NewsDataQueryBuilder`
    - Concatenar términos por prioridad con operador OR y comillas de frase; medir longitud de `q` antes de URL-encoding; mientras `>100` eliminar el término de menor prioridad (desempate por mayor índice) conservando el de mayor prioridad.
    - _Requirements: 3.2, 3.5, 4.6_

  - [ ]* 7.2 Escribir prueba de propiedades de longitud de la consulta
    - **Property 3: Longitud de la consulta q**
    - **Validates: Requirements 3.2, 3.5**

  - [ ]* 7.3 Escribir prueba de propiedades de conservación del término prioritario
    - **Property 4: Conservación del término de mayor prioridad**
    - **Validates: Requirements 3.2**

  - [ ]* 7.4 Escribir prueba de propiedades de determinismo e idempotencia
    - **Property 5: Determinismo e idempotencia del builder**
    - **Validates: Requirements 3.2**

- [x] 8. Capa de datos local: Room (entidades, DAO, migración)
  - [x] 8.1 Crear entidades `NewsArticleEntity`, `NewsLeagueCrossRefEntity` y `NewsLeagueMetaEntity`
    - Tablas `news_article`, `news_league_cross_ref` (N:M con índice por `league_key`), `news_league_meta` (`last_fetched_at_epoch_ms`).
    - _Requirements: 6.6, 6.1_

  - [x] 8.2 Implementar `NewsArticleDao`
    - Upsert de artículos + cross-ref, `setLastFetchedAt`/`lastFetchedAt` por `leagueKey`, listado por `leagueKey` ordenado por `published_at_epoch_utc DESC`, purga máx. 50 por liga (más antiguos por fecha).
    - _Requirements: 3.10, 6.6, 6.11_

  - [ ]* 8.3 Escribir prueba de propiedades de retención por liga
    - **Property 9: Retención máxima por liga** (Room in-memory)
    - **Validates: Requirements 6.11**

  - [x] 8.4 Migrar `GolIADatabase` v3→v4 y exportar schema 4
    - Subir `version = 4`, añadir las tres entidades al array `entities`, registrar `MIGRATION_3_4` en el builder y exportar `app/schemas/...GolIADatabase/4.json`.
    - _Requirements: 6.6_

  - [ ]* 8.5 Escribir `NewsMigrationTest` instrumentado (análogo a `MatchMigrationTest`)
    - Crear DB en v3, correr `MIGRATION_3_4`, validar tablas/columnas y conservación de datos preexistentes.
    - _Requirements: 6.6_

- [x] 9. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Repositorio: orquestación de datos
  - [x] 10.1 Implementar `NewsRepositoryImpl` y su binding en `RepositoryModule`
    - Orquestar `LeagueNewsQuery` → `NewsDataQueryBuilder` → `NewsDataApiService` → `NewsMapper` → `NewsArticleDao` con `NewsRequestBudgetManager`; conservar caché ante fallo; 429 → solo-caché; traducir fallos a `NewsError`; ejecutar en `@IoExecutor`. Añadir `@Binds NewsRepository`.
    - _Requirements: 3.1, 3.6, 3.9, 4.1, 6.1, 6.2, 6.5, 6.7, 6.8, 6.10, 11.4, 11.5_

  - [ ]* 10.2 Escribir pruebas unitarias del `NewsRepositoryImpl`
    - Éxito 200 (mapea, persiste, ordena); fallo/timeout conserva caché y devuelve error; 429/presupuesto agotado → solo-caché con `QuotaExceededError`; términos vacíos → consulta agregada.
    - _Requirements: 3.3, 3.6, 3.9, 6.7, 6.8, 6.10_

- [x] 11. Presentación: estado, ViewModel y filtro
  - [x] 11.1 Crear los modelos de UI `EstadoUi`, `ChipLiga` y `ArticleUiModel`
    - `EstadoUi { items, loading, banner(NINGUNO/OFFLINE/QUOTA/ERROR), empty(NINGUNO/LEAGUE/SEARCH) }` con equals/hashCode; `ChipLiga { leagueKey, label, selected }`.
    - _Requirements: 9.1, 10.1, 10.3_

  - [x] 11.2 Implementar `NoticiasViewModel` (`@HiltViewModel`)
    - Construir chips desde `TARGET_LEAGUE_IDS` + agrupación de eliminatorias + `Chip_Todos` (solo `Chip_Todos` si está vacío); selección única con reposición al primer elemento; consumo de casos de uso; observación vía `LiveData<EstadoUi>`; traducción de `Result`/`NewsError` a banners; guarda de reintento mientras `loading=true`; chequeo de `NEWSDATA_API_KEY` (ausente → `banner=ERROR` en ≤1s sin peticiones).
    - _Requirements: 1.1, 1.2, 1.5, 2.2, 2.3, 2.4, 2.5, 4.1, 5.5, 9.2, 9.3, 9.4, 9.5, 9.6, 10.1, 10.2, 10.3, 10.4_

  - [ ]* 11.3 Escribir prueba de propiedades de composición de chips
    - **Property 1: Composición y conteo de chips**
    - **Validates: Requirements 1.2, 1.3**

  - [x] 11.4 Implementar el filtro de búsqueda en el ViewModel
    - Debounce 300 ms cancelando el cálculo previo; corte a 256 caracteres; filtro por subcadena sobre título/descripción normalizados con `SearchTextNormalizer`; vacío/whitespace → lista completa; sin coincidencias → `empty=SEARCH`.
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8_

  - [ ]* 11.5 Escribir prueba de propiedades de correctitud del filtro
    - **Property 10: Correctitud del filtro de búsqueda**
    - **Validates: Requirements 8.2, 8.4**

  - [ ]* 11.6 Escribir prueba de propiedades del límite de longitud de búsqueda
    - **Property 11: Límite de longitud del texto de búsqueda**
    - **Validates: Requirements 8.6**

- [~] 12. Presentación: adaptador y vistas
  - [x] 12.1 Implementar `NewsAdapter` (`ListAdapter` + `DiffUtil`), `NewsDiffCallback` y ViewHolder
    - Bind con título ≤2 líneas, descripción ≤3 líneas, fuente ≤1 línea, fecha en formato corto de la zona local; ocultar campos ausentes sin dejar hueco; badge de liga/categoría; carga de imagen con Glide (placeholder, crossfade, timeout 10s).
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 11.3, 13.1_

  - [ ]* 12.2 Escribir pruebas unitarias del `NewsDiffCallback` y del formato de fecha
    - Ejemplos de inserción/borrado/cambio; formato de fecha con locale/zona fijos.
    - _Requirements: 7.6, 11.3_

  - [x] 12.3 Crear los layouts `fragment_noticias.xml` e `item_news.xml`
    - Tema oscuro/paleta, fila de chips, pull-to-refresh, estados carga/vacío(LEAGUE/SEARCH)/error/offline; `contentDescription` no vacía en búsqueda, chips y tarjetas.
    - _Requirements: 2.1, 2.6, 2.7, 9.1, 9.3, 12.6, 13.2_

- [x] 13. Cableado final: fragment y navegación
  - [x] 13.1 Conectar `NoticiasFragment` al ViewModel e integrarlo en la navegación existente
    - Observar `LiveData<EstadoUi>`, renderizar chips/lista/estados, delegar clicks (selección de chip, pull-to-refresh, icono de búsqueda, tap en artículo) al ViewModel, abrir la URL del artículo en navegador externo con fallback si no hay app capaz, sin invocar API/repositorio directamente.
    - _Requirements: 2.1, 2.2, 2.3, 4.2, 7.7, 7.8, 8.1, 10.1_

  - [ ]* 13.2 Escribir prueba de integración del flujo de refresco del repositorio a través del ViewModel
    - Verificar refresco al abrir/pull-to-refresh respetando presupuesto/intervalo y la exposición de banners (OFFLINE/QUOTA/ERROR) con caché intacta.
    - _Requirements: 6.1, 6.2, 6.7, 6.8, 10.1, 10.3_

- [x] 14. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Las tareas marcadas con `*` son opcionales (pruebas) y pueden omitirse para un MVP más rápido.
- Cada tarea referencia requisitos específicos para trazabilidad; las pruebas de propiedades referencian la Property correspondiente del diseño.
- El spike (tarea 1) es temprano porque puede refinar constantes de fecha, campos obligatorios, coste por crédito y el mapeo del error 429; el resto puede avanzar con los valores por defecto documentados.
- Los checkpoints aseguran validación incremental.
- El filtro de búsqueda es una transformación en memoria en el ViewModel (no un caso de uso de I/O), reutilizando `SearchTextNormalizer`.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2", "5.1", "6.1"] },
    { "id": 1, "tasks": ["3.1", "3.2", "4.1", "5.2", "5.3", "6.2", "6.3", "8.1"] },
    { "id": 2, "tasks": ["3.3", "3.4", "4.2", "5.4", "5.5", "6.4", "7.1", "8.2", "8.4"] },
    { "id": 3, "tasks": ["4.3", "4.4", "4.5", "7.2", "7.3", "7.4", "8.3", "8.5", "11.1"] },
    { "id": 4, "tasks": ["10.1", "11.2", "11.4", "12.1", "12.3"] },
    { "id": 5, "tasks": ["10.2", "11.3", "11.5", "11.6", "12.2", "13.1"] },
    { "id": 6, "tasks": ["13.2"] }
  ]
}
```
