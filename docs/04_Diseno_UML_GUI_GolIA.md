# Documento de Diseño: UML + GUI
## Aplicación GolIA — Pronósticos de Fútbol

**Autor:** [Tu Nombre] — Estudiante de Desarrollo de Software
**Fecha:** 2026

> Los diagramas usan sintaxis **Mermaid** y **PlantUML/ASCII**. Se renderizan en editores compatibles (VS Code con extensión Mermaid, GitHub, etc.).

---

## 1. Arquitectura general (diagrama de componentes)

```mermaid
flowchart LR
    subgraph Cliente["App Android (GolIA)"]
        UI["Capa Presentación\n(Activities, Fragments, ViewModels)"]
        DOM["Capa Dominio\n(Casos de uso, modelos, reglas)"]
        DAT["Capa Datos\n(Repositorios, Retrofit, Room)"]
        UI --> DOM --> DAT
    end

    subgraph Backend["Backend GolIA (Spring Boot)"]
        API["Controladores REST\n/auth /predictions /ranking /matches"]
        SRV["Servicios\n(Auth, Prediction, Resolution)"]
        REPO["Repositorios JPA"]
        API --> SRV --> REPO
    end

    DB[("MySQL\nusers / predictions")]
    FOOT["API-Football"]
    NEWS["NewsData.io"]

    DAT -->|HTTPS/JSON| API
    REPO --> DB
    DAT -->|datos de partidos| FOOT
    DAT -->|noticias| NEWS
```

---

## 2. Diagrama de clases (dominio simplificado)

```mermaid
classDiagram
    class User {
        +String id
        +String username
        +String email
        +String country
        +String avatarUrl
        +int totalPoints
        +int predictionsMade
        +int predictionsCorrect
    }

    class Match {
        +String id
        +String homeTeamName
        +String awayTeamName
        +long scheduledDateTime
        +MatchStatus status
        +Integer homeScore
        +Integer awayScore
    }

    class Prediction {
        +String id
        +String userId
        +String matchId
        +Integer predictedHomeScore
        +Integer predictedAwayScore
        +int pointsEarned
        +Boolean isCorrect
        +long createdAt
        +Long finalizedAt
    }

    class RankingEntry {
        +int position
        +String username
        +int totalPoints
        +int winPercentage
    }

    class PredictionScorer {
        +score(predH, predA, realH, realA) int
        +isCorrect(score) boolean
    }

    User "1" --> "0..*" Prediction : realiza
    Match "1" --> "0..*" Prediction : recibe
    PredictionScorer ..> Prediction : evalúa
    User ..> RankingEntry : se clasifica como
    Match --> MatchStatus

    class MatchStatus {
        <<enumeration>>
        SCHEDULED
        LIVE
        FINISHED
    }
```

---

## 3. Diagrama de secuencia: Realizar pronóstico (CU-06)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant V as PronosticoActivity
    participant VM as PronosticoViewModel
    participant UC as SavePredictionUseCase
    participant R as PredictionRepository (remoto)
    participant API as Backend /predictions
    participant DB as MySQL

    U->>V: Ingresa marcador (2-1) y confirma
    V->>VM: guardarPronostico(matchId, 2, 1)
    VM->>UC: execute(userId, matchId, 2, 1)
    UC->>UC: valida bloqueo por tiempo (>10 min)
    UC->>R: savePrediction(...)
    R->>API: POST /predictions {match_id, home, away}
    API->>DB: upsert por (user_id, match_id)
    DB-->>API: OK
    API-->>R: 200 PredictionDto
    R-->>UC: Result.Success(Prediction)
    UC-->>VM: Result.Success
    VM-->>V: navegar a Confirmación
    V-->>U: Muestra confirmación + puntos posibles
```

---

## 4. Diagrama de secuencia: Resolución de puntos (CU transversal RF-16/17)

```mermaid
sequenceDiagram
    participant APP as App (al refrescar partidos)
    participant R as RemotePredictionRepository
    participant API as Backend /matches/{id}/resolve
    participant RS as ResolutionService
    participant SC as PredictionScorer
    participant DB as MySQL

    APP->>R: resolveForMatch(partido FINISHED 3-1)
    R->>API: POST /matches/{id}/resolve {home:3, away:1}
    API->>RS: resolve(matchId, 3, 1)
    RS->>DB: cargar pronósticos del partido
    loop por cada pronóstico pendiente
        RS->>SC: score(pred, real)
        SC-->>RS: puntos (0..19)
        RS->>DB: actualizar prediction (puntos, is_correct)
    end
    RS->>DB: actualizar users (total_points, correct)
    DB-->>API: OK
    API-->>R: 200 ResolveResponse
```

---

## 5. Diagrama de navegación (flujo de pantallas)

```mermaid
flowchart TD
    Splash["Splash"] --> Login["Login"]
    Splash --> Main["Main (si hay sesión)"]
    Login --> Registro["Registro"]
    Registro --> Login
    Login --> Main

    Main --> Inicio["Inicio\n(bienvenida + próximos partidos + stats)"]
    Main --> Partidos["Partidos"]
    Main --> Ranking["Ranking\n(podio + lista + períodos)"]
    Main --> Noticias["Noticias"]
    Main --> Perfil["Perfil\n(stats + editar + foto + logout)"]

    Inicio --> Detalle["Detalle de Partido"]
    Partidos --> Detalle
    Detalle --> Pronostico["Pronóstico\n(marcador exacto)"]
    Pronostico --> Confirmacion["Confirmación"]
    Confirmacion --> Historial["Historial de Pronósticos"]
    Ranking --> Historial
    Perfil --> Login
```

---

## 6. Modelo entidad-relación (base de datos MySQL)

```mermaid
erDiagram
    USERS ||--o{ PREDICTIONS : "realiza"
    USERS {
        varchar id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar country
        varchar avatar_url
        int total_points
        int predictions_made
        int predictions_correct
        bigint created_at
    }
    PREDICTIONS {
        varchar id PK
        varchar user_id FK
        varchar match_id
        varchar predicted_outcome
        int predicted_home_score
        int predicted_away_score
        int points_earned
        boolean is_correct
        bigint created_at
        bigint finalized_at
    }
```
> Restricción: índice único `(user_id, match_id)` → un pronóstico por usuario y partido.

---

## 7. Diseño de la Interfaz Gráfica (GUI)

### 7.1 Mapa de pantallas

| Pantalla | Archivo layout | Función principal |
|---|---|---|
| Splash | `activity_splash.xml` | Carga inicial y decisión de sesión. |
| Login | `activity_login.xml` | Autenticación. |
| Registro | `activity_registro.xml` | Alta de cuenta. |
| Main (contenedor) | `activity_main.xml` | Barra de navegación inferior. |
| Inicio | `fragment_inicio.xml` | Bienvenida, estadísticas y próximos partidos. |
| Partidos | `fragment_partidos.xml` | Lista de partidos. |
| Ranking | `fragment_ranking.xml` | Podio, lista y filtro por período + historial. |
| Noticias | `fragment_noticias.xml` | Feed de noticias. |
| Perfil | `fragment_perfil.xml` | Estadísticas, edición, foto y logout. |
| Detalle de partido | `activity_detalle_partido.xml` | Información del partido. |
| Pronóstico | `activity_pronostico.xml` | Ingreso de marcador. |
| Confirmación | `activity_confirmacion.xml` | Resumen del pronóstico. |
| Historial | `activity_historial_pronosticos.xml` | Lista de pronósticos. |

### 7.2 Principios de diseño aplicados
- **Material Design** con paleta oscura azul.
- **Navegación por barra inferior** de 5 secciones.
- **Estados visuales**: carga (progress), vacío (mensaje), error (reintentar).
- **Jerarquía visual** en el ranking: podio destacado + lista.
- **Accesibilidad**: descripciones de contenido en íconos e imágenes.

### 7.3 Wireframes
Los wireframes de las pantallas principales se incluyen como artefactos visuales adjuntos (ver `wireframes_golia.svg`).
