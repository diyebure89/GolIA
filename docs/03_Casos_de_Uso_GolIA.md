# Documento de Casos de Uso
## Aplicación GolIA — Pronósticos de Fútbol

**Autor:** Diego Yeisd Builes Restrepo — Aprendiz, Técnico en programación de software (SENA)
**Fecha:** 2026

---

## 1. Actores del sistema

| Actor | Descripción |
|---|---|
| **Usuario registrado** | Persona autenticada que pronostica, consulta ranking, historial, noticias y gestiona su perfil. |
| **Usuario invitado** | Persona sin sesión; puede ver contenido público (ranking). |
| **API-Football** (actor externo) | Servicio que provee partidos y resultados. |
| **NewsData.io** (actor externo) | Servicio que provee noticias. |
| **Backend GolIA** (actor externo/sistema) | Servicio REST que gestiona cuentas, pronósticos, puntos y ranking en MySQL. |

---

## 2. Diagrama de casos de uso (vista general)

```
                         ┌───────────────────────────────────────────────┐
                         │                 Sistema GolIA                  │
                         │                                               │
                         │   (CU-01) Registrarse                         │
                         │   (CU-02) Iniciar sesión                      │
                         │   (CU-03) Cerrar sesión                       │
                         │   (CU-04) Ver próximos partidos               │
   ┌──────────────┐      │   (CU-05) Ver detalle de partido              │     ┌───────────────┐
   │   Usuario    │──────│   (CU-06) Realizar pronóstico                 │─────│ API-Football  │
   │  registrado  │      │   (CU-07) Ver historial de pronósticos        │     └───────────────┘
   └──────────────┘      │   (CU-08) Consultar ranking                   │
          │              │   (CU-09) Ver noticias                        │     ┌───────────────┐
          │              │   (CU-10) Gestionar perfil                    │─────│ NewsData.io   │
          │              │   (CU-11) Cambiar foto de perfil              │     └───────────────┘
          │              │                                               │
   ┌──────────────┐      │                                               │     ┌───────────────┐
   │   Usuario    │──────│   (CU-08) Consultar ranking (público)         │─────│ Backend GolIA │
   │   invitado   │      │                                               │     └───────────────┘
   └──────────────┘      └───────────────────────────────────────────────┘
```

**Relaciones de inclusión/extensión:**
- CU-06 (Realizar pronóstico) `<<include>>` CU-05 (Ver detalle de partido).
- CU-06 `<<extend>>` Confirmar pronóstico (pantalla de confirmación).
- CU-04, CU-05, CU-06, CU-07, CU-10, CU-11 requieren sesión (precondición: CU-02).

---

## 3. Especificación de casos de uso

### CU-01 — Registrarse
| Campo | Detalle |
|---|---|
| **Actor** | Usuario invitado |
| **Descripción** | Crear una cuenta nueva en el sistema. |
| **Precondición** | El usuario no tiene sesión activa. |
| **Flujo principal** | 1. El usuario abre la pantalla de registro. 2. Ingresa nombre de usuario, correo, contraseña y país. 3. El sistema valida formato de correo y contraseña (≥8 caracteres). 4. El sistema envía la solicitud al backend. 5. El backend verifica unicidad de correo/usuario y crea la cuenta. 6. El sistema confirma el registro y dirige al login. |
| **Flujo alternativo** | 3a. Datos inválidos → se muestran errores por campo. 5a. Correo o usuario ya existe → mensaje de conflicto. 4a. Sin conexión → mensaje de error de red. |
| **Postcondición** | La cuenta queda creada en MySQL. |
| **Requisitos** | RF-01, RF-02, RF-06 |

### CU-02 — Iniciar sesión
| Campo | Detalle |
|---|---|
| **Actor** | Usuario invitado |
| **Descripción** | Autenticarse en el sistema. |
| **Precondición** | El usuario posee una cuenta. |
| **Flujo principal** | 1. El usuario ingresa usuario/correo y contraseña. 2. El sistema envía la solicitud al backend. 3. El backend valida credenciales y responde con token JWT y datos del usuario. 4. El sistema guarda la sesión y navega a Inicio. |
| **Flujo alternativo** | 3a. Credenciales inválidas → mensaje genérico sin revelar el campo. 2a. Sin conexión → error de red. |
| **Postcondición** | Sesión activa con token válido. |
| **Requisitos** | RF-03, RF-05, RF-06 |

### CU-03 — Cerrar sesión
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Finalizar la sesión actual. |
| **Precondición** | Sesión activa. |
| **Flujo principal** | 1. El usuario pulsa "Cerrar sesión" en Perfil. 2. El sistema limpia tokens y datos de sesión. 3. Navega a la pantalla de login. |
| **Postcondición** | No hay sesión activa. |
| **Requisitos** | RF-04 |

### CU-04 — Ver próximos partidos
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Consultar los partidos disponibles. |
| **Precondición** | Sesión activa. |
| **Flujo principal** | 1. El usuario abre Inicio o Partidos. 2. El sistema pide los partidos (caché primero y refresco desde API-Football). 3. Muestra la lista ordenada por fecha, con estado (programado/en vivo/finalizado). |
| **Flujo alternativo** | 2a. Sin conexión → muestra la caché local. 2b. Sin datos → estado vacío. |
| **Postcondición** | Lista de partidos visible. |
| **Requisitos** | RF-07, RF-08, RF-10 |

### CU-05 — Ver detalle de partido
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Consultar la información de un partido. |
| **Precondición** | Sesión activa; partido existente. |
| **Flujo principal** | 1. El usuario pulsa un partido. 2. El sistema abre el detalle con equipos, fecha, estado y marcador si aplica. 3. Ofrece la opción de pronosticar si el partido está abierto. |
| **Postcondición** | Detalle visible. |
| **Requisitos** | RF-09, RF-10 |

### CU-06 — Realizar pronóstico
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Predecir el marcador exacto de un partido. |
| **Precondición** | Sesión activa; partido programado a más de 10 minutos del inicio. |
| **Flujo principal** | 1. Desde el detalle, el usuario pulsa "Pronosticar". 2. Ingresa goles local y visitante. 3. El sistema valida el bloqueo por tiempo. 4. Envía el pronóstico al backend (crea o actualiza). 5. Muestra la pantalla de confirmación con el marcador y los puntos posibles. |
| **Flujo alternativo** | 3a. Partido cerrado/por iniciar → mensaje "pronóstico cerrado". 4a. Sin conexión → error. 2a. Valores fuera de rango → error de validación. |
| **Postcondición** | Pronóstico almacenado en MySQL (único por usuario y partido). |
| **Requisitos** | RF-11, RF-12, RF-13, RF-14 |

### CU-07 — Ver historial de pronósticos
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Consultar los pronósticos realizados y su estado. |
| **Precondición** | Sesión activa. |
| **Flujo principal** | 1. El usuario abre el historial (desde confirmación o ranking). 2. El sistema lista los pronósticos con partido, marcador, estado (pendiente/acertado/fallido) y puntos. |
| **Flujo alternativo** | 2a. Sin pronósticos → estado vacío. 2b. Partido no disponible en caché → "Partido no disponible". |
| **Postcondición** | Historial visible. |
| **Requisitos** | RF-15 |

### CU-08 — Consultar ranking
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado / Usuario invitado |
| **Descripción** | Ver la clasificación de usuarios por puntos. |
| **Precondición** | Ninguna para ver el ranking público. |
| **Flujo principal** | 1. El usuario abre Ranking. 2. El sistema solicita el ranking global al backend. 3. Muestra el podio (3 primeros) y la lista ordenada por puntos. 4. El usuario puede filtrar por período (Semanal/Mensual/Total). |
| **Flujo alternativo** | 2a. Sin conexión → cálculo/estado local. 2b. Sin usuarios → estado vacío. |
| **Postcondición** | Ranking visible y filtrable. |
| **Requisitos** | RF-18, RF-19, RF-20, RF-21 |

### CU-09 — Ver noticias
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Leer noticias deportivas. |
| **Precondición** | Sesión activa. |
| **Flujo principal** | 1. El usuario abre Noticias. 2. El sistema obtiene las noticias (caché + NewsData.io). 3. Muestra la lista. 4. Al pulsar una noticia se abre su fuente original. |
| **Flujo alternativo** | 2a. Cuota agotada/sin conexión → muestra caché. |
| **Postcondición** | Noticias visibles. |
| **Requisitos** | RF-22, RF-23 |

### CU-10 — Gestionar perfil
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Ver estadísticas y editar datos personales / contraseña. |
| **Precondición** | Sesión activa. |
| **Flujo principal** | 1. El usuario abre Perfil. 2. El sistema muestra estadísticas (pronósticos, aciertos %, puntos, ranking). 3. El usuario edita usuario/correo y guarda. 4. Opcionalmente cambia la contraseña (actual + nueva + confirmación). |
| **Flujo alternativo** | 3a. Correo/usuario en uso → error. 4a. Contraseña actual incorrecta → error. |
| **Postcondición** | Datos actualizados. |
| **Requisitos** | RF-24, RF-25, RF-26, RF-28 |

### CU-11 — Cambiar foto de perfil
| Campo | Detalle |
|---|---|
| **Actor** | Usuario registrado |
| **Descripción** | Establecer una foto de perfil. |
| **Precondición** | Sesión activa; permisos de cámara/almacenamiento según la opción. |
| **Flujo principal** | 1. El usuario pulsa editar foto. 2. Elige galería o cámara. 3. El sistema obtiene la imagen y la guarda. 4. Actualiza el avatar en Perfil e Inicio. |
| **Flujo alternativo** | 2a. Permiso de cámara denegado → no se abre la cámara. |
| **Postcondición** | Avatar actualizado. |
| **Requisitos** | RF-27 |

---

## 4. Matriz de trazabilidad (Caso de uso ↔ Requisitos)

| Caso de uso | Requisitos cubiertos |
|---|---|
| CU-01 | RF-01, RF-02, RF-06 |
| CU-02 | RF-03, RF-05, RF-06 |
| CU-03 | RF-04 |
| CU-04 | RF-07, RF-08, RF-10 |
| CU-05 | RF-09, RF-10 |
| CU-06 | RF-11, RF-12, RF-13, RF-14 |
| CU-07 | RF-15 |
| CU-08 | RF-18, RF-19, RF-20, RF-21 |
| CU-09 | RF-22, RF-23 |
| CU-10 | RF-24, RF-25, RF-26, RF-28 |
| CU-11 | RF-27 |
| (Transversal: resolución de puntos) | RF-16, RF-17 |
