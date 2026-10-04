# GolIA Backend (Spring Boot + MySQL)

API REST que respalda la app GolIA con **MySQL**, para que los datos (cuentas,
pronósticos y ranking) se compartan entre **todos los dispositivos**. La cuenta
vive en el servidor, no en el teléfono: el mismo usuario puede iniciar sesión
desde cualquier dispositivo y ver el mismo ranking global.

Este backend está alineado con los contratos que la app Android ya espera
(`AuthApiService` / `AuthDto`), así que los nombres JSON coinciden exactamente.

## Alcance (enfoque mínimo)

- **Usuarios**: registro, login, refresh de token, perfil.
- **Pronósticos**: crear/actualizar (upsert por `user_id + match_id`), historial
  del usuario, listado por partido.
- **Ranking global**: usuarios ordenados por puntos.

Los **partidos y noticias** siguen viniendo de las APIs externas (API-Football /
NewsData.io); no se migran a este MySQL.

## Requisitos

- **JDK 17 o superior** (probado con JDK 25).
- **MySQL** (local para desarrollo, o Aiven para compartir entre dispositivos).
- Maven. Si no lo tienes instalado globalmente, este proyecto trae una copia
  portátil descargada en `tools/apache-maven-3.9.9/` (no versionada). Los
  comandos de abajo usan esa copia.

## 1. Configurar MySQL en Aiven (gratis, always free)

1. Crea una cuenta en https://aiven.io (no pide tarjeta).
2. Crea un servicio **MySQL** en el plan **Free**.
3. Cuando esté listo, copia del panel: **host**, **puerto**, **usuario**,
   **contraseña** y **nombre de la base** (`defaultdb` por defecto).
4. Aiven exige SSL, así que la URL JDBC lleva `?ssl-mode=REQUIRED`.

## 2. Variables de entorno

Las credenciales **no se hardcodean**: se leen de variables de entorno. Define
estas antes de arrancar (ejemplo para Aiven):

| Variable | Ejemplo | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://<host>:<puerto>/defaultdb?ssl-mode=REQUIRED&serverTimezone=UTC` | Cadena JDBC |
| `DB_USER` | `avnadmin` | Usuario de MySQL |
| `DB_PASSWORD` | `********` | Contraseña de MySQL |
| `GOLIA_JWT_SECRET` | (cadena larga y aleatoria, ≥32 bytes) | Clave para firmar los JWT |
| `PORT` | `8080` | Puerto del servidor (opcional) |

En Windows PowerShell (sesión actual):

```powershell
$env:DB_URL="jdbc:mysql://TU_HOST:TU_PUERTO/defaultdb?ssl-mode=REQUIRED&serverTimezone=UTC"
$env:DB_USER="avnadmin"
$env:DB_PASSWORD="TU_PASSWORD"
$env:GOLIA_JWT_SECRET="pon-aqui-una-clave-larga-y-aleatoria-de-al-menos-32-bytes"
```

Para desarrollo local sin Aiven, los valores por defecto de
`application.properties` apuntan a un MySQL local (`localhost:3306`, base
`golia` que se crea sola).

## 3. Compilar y ejecutar

Con el Maven portátil incluido (desde la carpeta `backend/`):

```powershell
# Compilar
tools\apache-maven-3.9.9\bin\mvn.cmd -DskipTests clean package

# Ejecutar
tools\apache-maven-3.9.9\bin\mvn.cmd -DskipTests spring-boot:run
```

Si tienes Maven instalado globalmente, basta con `mvn spring-boot:run`.

El esquema de MySQL (tablas `users` y `predictions`, con sus índices únicos) se
crea/actualiza automáticamente al arrancar (`spring.jpa.hibernate.ddl-auto=update`).

El servidor queda escuchando en `http://localhost:8080`.

## 4. Endpoints

Todos devuelven/reciben JSON. Los que requieren sesión esperan el header
`Authorization: Bearer <access_token>`.

### Auth (públicos)
| Método | Ruta | Cuerpo | Descripción |
|---|---|---|---|
| POST | `/auth/register` | `{ "username", "email", "password", "country" }` | Crea cuenta y devuelve tokens |
| POST | `/auth/login` | `{ "email", "password" }` | `email` acepta username o correo |
| POST | `/auth/refresh` | `{ "refresh_token" }` | Renueva el access token |
| POST | `/auth/logout` | — | 200 OK (la app borra sus tokens) |

Respuesta de auth: `{ "access_token", "refresh_token", "user": {...}, "expires_in" }`.

### Usuario / pronósticos (requieren token)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/user/profile` | Perfil del usuario autenticado |
| POST | `/predictions` | Upsert: `{ "match_id", "home_score", "away_score" }` |
| GET | `/predictions` | Historial del usuario autenticado |
| GET | `/predictions/by-match?matchId=...` | Pronósticos de un partido |

### Ranking (público)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/ranking` | Ranking global por puntos (posición 1..N) |

## 5. Desplegar (para que sea accesible desde los dispositivos)

Para uso local en la misma red, la app puede apuntar a la IP de tu PC. Para uso
real entre dispositivos en cualquier lado, despliega el backend en un host
gratuito (Render, Railway, etc.):

1. Sube esta carpeta `backend/` a un repositorio.
2. En el host, crea un servicio web Java/Maven.
3. Define las mismas variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`,
   `GOLIA_JWT_SECRET`) en el panel del host.
4. El host construye con `mvn package` y ejecuta el `.jar` generado en
   `target/`.

## 6. Conectar la app Android

En `NetworkModule.java` de la app, cambia `GOLIA_BASE_URL` de
`https://api.golia.app/v1/` a la URL pública de tu backend (con `/` final, sin
prefijo `/v1` salvo que lo agregues). Luego, activar el camino remoto de auth y
predicciones en la app es la **Fase 2** (todavía no hecha): implica cambiar los
bindings de Hilt para usar `AuthRepositoryImpl` (remoto) en vez del local y
añadir el servicio Retrofit de predicciones/ranking.

## Notas de seguridad

- Las contraseñas se guardan **hasheadas con BCrypt**, nunca en claro.
- Los tokens son **JWT** firmados con `GOLIA_JWT_SECRET` (HS256). Usa una clave
  larga y secreta en producción.
- La API es **stateless**: no hay sesiones de servidor; la identidad viaja en el
  token.
