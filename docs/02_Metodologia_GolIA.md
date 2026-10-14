# Metodología de Desarrollo
## Aplicación GolIA — Pronósticos de Fútbol

**Proyecto:** GolIA (Android + Backend REST/MySQL)
**Autor:** Diego Yeisd Builes Restrepo — Aprendiz, Técnico en programación de software (SENA)
**Fecha:** 2026

---

## 1. Metodología seleccionada: Desarrollo por Prototipos (evolutivo)

Para GolIA se seleccionó la **metodología por prototipos de tipo evolutivo**, dentro de un marco de trabajo iterativo e incremental.

### 1.1 Justificación de la elección

Se eligió el **modelo por prototipos** y no un modelo puramente en cascada ni un aplicativo cerrado por las siguientes razones:

1. **Requisitos que se refinan con el uso.** Las reglas de negocio (puntuación, períodos del ranking, bloqueo de pronósticos) se afinaron a medida que se veía el comportamiento real de la app, algo propio del enfoque por prototipos.
2. **Validación temprana de la interfaz.** Construir pantallas funcionales (Inicio, Partidos, Ranking, Perfil) permitió validar la experiencia antes de invertir en la lógica completa.
3. **Integraciones externas inciertas.** Las APIs de terceros (API-Football, NewsData.io) y el backend propio se integraron de forma incremental, probando cada pieza en un prototipo antes de consolidarla.
4. **Evolución del prototipo en producto.** Cada prototipo no se descartó: se fue refinando hasta convertirse en la aplicación final (prototipado **evolutivo**).

### 1.2 Carácter evolutivo (no desechable)
Los prototipos de GolIA se construyeron sobre una **arquitectura por capas estable** (presentación, dominio y datos) con inyección de dependencias. Esto permitió que cada prototipo se mejorara sin reescribir el núcleo: por ejemplo, la capa de datos pasó de almacenamiento **local (Room/SQLite)** a un **backend remoto (Spring Boot + MySQL)** cambiando únicamente las implementaciones de repositorio, sin tocar la interfaz ni la lógica de dominio.

---

## 2. Ciclo de vida por prototipos aplicado a GolIA

```
   ┌─────────────────────────────────────────────────────────┐
   │  1. Recolección y refinamiento de requisitos             │
   │     (necesidades: pronosticar, competir, ver partidos)   │
   └───────────────────────────┬─────────────────────────────┘
                               ▼
   ┌─────────────────────────────────────────────────────────┐
   │  2. Diseño rápido (pantallas + modelo de datos)          │
   └───────────────────────────┬─────────────────────────────┘
                               ▼
   ┌─────────────────────────────────────────────────────────┐
   │  3. Construcción del prototipo (iteración N)             │
   └───────────────────────────┬─────────────────────────────┘
                               ▼
   ┌─────────────────────────────────────────────────────────┐
   │  4. Evaluación del prototipo (pruebas + feedback)        │
   └───────────────────────────┬─────────────────────────────┘
                               ▼
   ┌─────────────────────────────────────────────────────────┐
   │  5. Refinamiento  ──► (vuelve al paso 1 si hay cambios)   │
   └───────────────────────────┬─────────────────────────────┘
                               ▼
   ┌─────────────────────────────────────────────────────────┐
   │  6. Producto final (prototipo consolidado)               │
   └─────────────────────────────────────────────────────────┘
```

---

## 3. Iteraciones realizadas (incrementos)

| Iteración | Objetivo del prototipo | Resultado |
|---|---|---|
| **Iteración 1** | Autenticación local y navegación base | Registro/login con Room, barra de navegación inferior, pantallas vacías. |
| **Iteración 2** | Partidos y detalle | Consumo de API-Football, listado y detalle de partidos, caché local. |
| **Iteración 3** | Pronósticos | Formulario de marcador exacto, bloqueo por tiempo, confirmación e historial. |
| **Iteración 4** | Puntuación y ranking | Regla de puntuación, ranking por períodos y podio. |
| **Iteración 5** | Perfil y noticias | Edición de datos, foto de perfil, estadísticas y feed de noticias. |
| **Iteración 6** | Backend compartido | Migración de usuarios, pronósticos y ranking a Spring Boot + MySQL (datos entre dispositivos). |
| **Iteración 7** | Resolución de puntos en servidor | Endpoint de resolución idempotente que actualiza puntos globales. |

---

## 4. Tecnologías y herramientas

### 4.1 Cliente (Android)
- **Lenguaje:** Java 17.
- **UI:** Android SDK, Material Design, ViewBinding, Navigation, Lottie.
- **Arquitectura:** MVVM por capas (presentación / dominio / datos).
- **Inyección de dependencias:** Hilt (Dagger).
- **Red:** Retrofit + OkHttp + Gson.
- **Persistencia local (caché):** Room (SQLite).
- **Imágenes:** Glide.

### 4.2 Servidor (Backend)
- **Framework:** Spring Boot 3.4 (Java 17).
- **Persistencia:** Spring Data JPA + Hibernate sobre MySQL/MariaDB.
- **Seguridad:** Spring Security + JWT (jjwt), hash BCrypt.
- **Build:** Maven.

### 4.3 Servicios externos
- **API-Football:** datos de partidos y resultados.
- **NewsData.io:** noticias deportivas.

### 4.4 Herramientas de desarrollo
- IDE/Editor con asistencia de IA.
- Control de versiones Git.
- XAMPP (MySQL) para entorno de desarrollo local.

---

## 5. Buenas prácticas aplicadas
- **Separación de responsabilidades** por capas y módulos.
- **Programación contra interfaces** (repositorios de dominio) para poder cambiar implementaciones (local ↔ remoto).
- **Seguridad por defecto:** credenciales fuera del repositorio, hash de contraseñas, token cifrado.
- **Operaciones idempotentes** en la resolución de puntos.
- **Degradación controlada:** si el backend no responde, la UI muestra estados neutros/caché sin romper.

---

## 6. Conclusión
El enfoque por prototipos evolutivos permitió entregar valor de forma incremental, validar la experiencia tempranamente y evolucionar la arquitectura (de local a cliente-servidor) sin reescrituras, cumpliendo los requisitos priorizados del documento ERS.
