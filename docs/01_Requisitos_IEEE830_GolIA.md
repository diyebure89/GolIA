# Especificación de Requisitos de Software (ERS)
## Aplicación GolIA — Pronósticos de Fútbol

**Estándar de referencia:** IEEE 830-1998
**Proyecto:** GolIA (Android)
**Versión del documento:** 1.0
**Autor:** [Tu Nombre] — Estudiante de Desarrollo de Software
**Fecha:** 2026

---

## 1. Introducción

### 1.1 Propósito
Este documento especifica los requisitos funcionales y no funcionales de la aplicación móvil **GolIA**, una plataforma de pronósticos deportivos de fútbol. Está dirigido al equipo de desarrollo, al docente evaluador y a cualquier interesado en comprender el alcance del sistema.

### 1.2 Alcance
GolIA permite a los usuarios registrarse, consultar partidos de fútbol de ligas objetivo, realizar pronósticos de marcador exacto, acumular puntos según el acierto, competir en un ranking global compartido entre dispositivos y leer noticias deportivas. El sistema está compuesto por:

- **Aplicación Android** (cliente): interfaz de usuario y lógica de presentación.
- **Backend REST (Spring Boot + MySQL):** autenticación, almacenamiento de usuarios, pronósticos y ranking compartido entre dispositivos.
- **Integraciones externas:** API-Football (datos de partidos) y NewsData.io (noticias).

### 1.3 Definiciones, acrónimos y abreviaturas

| Término | Definición |
|---|---|
| Pronóstico | Predicción del marcador exacto de un partido realizada por el usuario. |
| Marcador exacto | Resultado numérico de goles local–visitante (ej. 2-1). |
| Ranking | Clasificación de usuarios ordenada por puntos acumulados. |
| Período | Ventana temporal del ranking: Semanal (7 días), Mensual (30 días) o Total. |
| JWT | JSON Web Token, mecanismo de autenticación por token. |
| API | Interfaz de Programación de Aplicaciones. |
| ERS | Especificación de Requisitos de Software. |
| RF | Requisito Funcional. |
| RNF | Requisito No Funcional. |

### 1.4 Referencias
- IEEE Std 830-1998, Recommended Practice for Software Requirements Specifications.
- Documentación API-Football (v3.football.api-sports.io).
- Documentación NewsData.io.

### 1.5 Visión general
La sección 2 describe el contexto general del producto. La sección 3 detalla los requisitos específicos (funcionales y no funcionales). La sección 4 presenta la priorización.

---

## 2. Descripción general

### 2.1 Perspectiva del producto
GolIA es una aplicación cliente-servidor. El cliente Android consume una API REST propia para la gestión de cuentas, pronósticos y ranking, y APIs de terceros para partidos y noticias. Los datos de cuenta y pronósticos residen en una base de datos MySQL central, lo que permite que un mismo usuario acceda desde distintos dispositivos y vea un ranking consistente.

### 2.2 Funciones del producto (resumen)
- Registro e inicio de sesión de usuarios.
- Consulta de próximos partidos y detalle de partido.
- Realización de pronósticos de marcador exacto con bloqueo por tiempo.
- Cálculo de puntos por acierto.
- Ranking global por períodos.
- Historial de pronósticos del usuario.
- Lectura de noticias deportivas.
- Gestión de perfil (datos, contraseña y foto).

### 2.3 Características de los usuarios

| Perfil | Descripción |
|---|---|
| Usuario registrado | Persona aficionada al fútbol que realiza pronósticos y compite en el ranking. Conocimientos tecnológicos básicos. |
| Usuario invitado | Puede ver contenido público (p. ej. ranking) sin sesión activa, pero no pronosticar. |

### 2.4 Restricciones
- Plataforma Android (minSdk 24, targetSdk 36).
- Dependencia de conectividad para auth, pronósticos, ranking y datos externos.
- Límite de peticiones de las APIs gratuitas de terceros (presupuesto diario).
- La contraseña debe tener mínimo 8 caracteres.

### 2.5 Suposiciones y dependencias
- El backend GolIA y la base de datos MySQL están disponibles y accesibles por red.
- Las claves de API de terceros son válidas y no exceden su cuota.
- El dispositivo cuenta con conexión a Internet.

---

## 3. Requisitos específicos

> **Nota conceptual:** El **requerimiento** es la necesidad o deseo del cliente (ej. "quiero competir con otros"); el **requisito** es la especificación técnica detallada que el sistema debe cumplir para satisfacer esa necesidad (ej. "el sistema debe mostrar un ranking ordenado por puntos descendente, actualizado por período").

### 3.1 Requisitos Funcionales (RF)

#### Módulo: Autenticación

| ID | Requisito | Descripción |
|---|---|---|
| RF-01 | Registro de usuario | El sistema debe permitir crear una cuenta con nombre de usuario, correo, contraseña y país. El correo y el usuario deben ser únicos. |
| RF-02 | Validación de registro | El sistema debe validar que el correo tenga formato válido y que la contraseña tenga al menos 8 caracteres antes de enviar la solicitud. |
| RF-03 | Inicio de sesión | El sistema debe permitir autenticarse con usuario/correo y contraseña, devolviendo un token de sesión (JWT). |
| RF-04 | Cierre de sesión | El sistema debe permitir cerrar la sesión y limpiar los datos de sesión locales. |
| RF-05 | Persistencia de sesión | El sistema debe mantener la sesión activa entre aperturas de la app mientras el token sea válido. |
| RF-06 | Mensajes de error de auth | El sistema debe informar de credenciales inválidas, correo/usuario ya en uso y errores de conexión, sin revelar qué campo falló en el login. |

#### Módulo: Partidos

| ID | Requisito | Descripción |
|---|---|---|
| RF-07 | Listado de próximos partidos | El sistema debe mostrar en Inicio una lista de próximos partidos de las ligas objetivo. |
| RF-08 | Pantalla de partidos | El sistema debe mostrar la lista completa de partidos con pull-to-refresh. |
| RF-09 | Detalle de partido | El sistema debe mostrar el detalle de un partido (equipos, fecha, estado, marcador si aplica). |
| RF-10 | Datos en vivo | El sistema debe reflejar el estado del partido (programado, en vivo con minuto, finalizado). |

#### Módulo: Pronósticos

| ID | Requisito | Descripción |
|---|---|---|
| RF-11 | Crear pronóstico | El sistema debe permitir ingresar un marcador exacto (goles local y visitante) para un partido. |
| RF-12 | Bloqueo por tiempo | El sistema debe impedir pronosticar un partido a menos de 10 minutos de su inicio o si no está programado. |
| RF-13 | Pronóstico único | El sistema debe permitir un solo pronóstico por usuario y partido; un nuevo envío actualiza el existente (upsert). |
| RF-14 | Confirmación | El sistema debe mostrar una pantalla de confirmación con el marcador pronosticado y los puntos posibles. |
| RF-15 | Historial de pronósticos | El sistema debe mostrar el historial de pronósticos del usuario con su estado (pendiente, acertado, fallido) y puntos. |

#### Módulo: Puntuación y Ranking

| ID | Requisito | Descripción |
|---|---|---|
| RF-16 | Cálculo de puntos | El sistema debe calcular los puntos de un pronóstico al finalizar el partido según la regla: +5 por acertar el resultado 1X2, +2 por los goles locales exactos, +2 por los goles visitantes exactos, +1 por la diferencia de goles y +9 por el marcador exacto completo (máximo 19). |
| RF-17 | Resolución automática | El sistema debe resolver los pronósticos pendientes de un partido finalizado y actualizar los puntos del usuario. |
| RF-18 | Ranking global | El sistema debe mostrar un ranking de usuarios ordenado por puntos de forma descendente. |
| RF-19 | Períodos del ranking | El sistema debe permitir filtrar el ranking por período: Semanal, Mensual y Total. |
| RF-20 | Podio | El sistema debe destacar los tres primeros lugares del ranking (podio). |
| RF-21 | Datos reales compartidos | El ranking debe reflejar a los usuarios reales almacenados en el servidor, compartidos entre dispositivos. |

#### Módulo: Noticias

| ID | Requisito | Descripción |
|---|---|---|
| RF-22 | Listado de noticias | El sistema debe mostrar una lista de noticias deportivas de fútbol. |
| RF-23 | Apertura de noticia | El sistema debe permitir abrir la noticia en su fuente original. |

#### Módulo: Perfil

| ID | Requisito | Descripción |
|---|---|---|
| RF-24 | Ver estadísticas | El sistema debe mostrar en el perfil las métricas del usuario: pronósticos realizados, % de aciertos, puntos y posición en el ranking. |
| RF-25 | Editar datos | El sistema debe permitir editar el nombre de usuario y el correo. |
| RF-26 | Cambiar contraseña | El sistema debe permitir cambiar la contraseña validando la actual y la confirmación de la nueva. |
| RF-27 | Foto de perfil | El sistema debe permitir establecer una foto de perfil desde la galería o la cámara. |
| RF-28 | Bienvenida personalizada | El sistema debe mostrar en Inicio un saludo con el nombre y avatar del usuario. |

### 3.2 Requisitos No Funcionales (RNF)

| ID | Categoría | Requisito |
|---|---|---|
| RNF-01 | Usabilidad | La interfaz debe seguir Material Design y una navegación por barra inferior (Inicio, Partidos, Ranking, Noticias, Perfil). |
| RNF-02 | Usabilidad | Las operaciones deben mostrar estados de carga, vacío y error claros, sin textos técnicos. |
| RNF-03 | Rendimiento | Las operaciones de red deben ejecutarse fuera del hilo principal y responder o fallar en un tiempo acotado (timeout). |
| RNF-04 | Seguridad | Las contraseñas deben almacenarse con hash (BCrypt) en el servidor; nunca en texto plano. |
| RNF-05 | Seguridad | La autenticación debe basarse en tokens JWT; los endpoints protegidos requieren token válido. |
| RNF-06 | Seguridad | El token de sesión debe guardarse cifrado en el dispositivo (Android Keystore). |
| RNF-07 | Compatibilidad | La app debe funcionar desde Android 7.0 (API 24) en adelante. |
| RNF-08 | Mantenibilidad | El código debe seguir una arquitectura por capas (presentación, dominio, datos) con inyección de dependencias (Hilt). |
| RNF-09 | Confiabilidad | La resolución de puntos debe ser idempotente: reprocesar un partido no debe duplicar puntos. |
| RNF-10 | Disponibilidad offline | El contenido secundario (partidos y noticias) debe cachearse localmente para una experiencia degradada sin conexión. |
| RNF-11 | Escalabilidad | El modelo de datos debe permitir múltiples usuarios compartiendo el mismo ranking desde distintos dispositivos. |
| RNF-12 | Portabilidad | Las credenciales y URLs deben ser configurables por entorno (no hardcodeadas en el repositorio). |

---

## 4. Priorización de requisitos

La priorización ordena las funcionalidades de **mayor a menor valor estratégico**, considerando el valor para el usuario, la dependencia técnica y el riesgo. Se usa la escala **MoSCoW** (Must / Should / Could / Won't) combinada con un nivel Alta/Media/Baja.

### 4.1 Tabla de priorización

| Prioridad | MoSCoW | Requisitos | Justificación |
|---|---|---|---|
| **1 — Alta** | Must | RF-01, RF-02, RF-03, RF-05, RF-06 | Sin autenticación no hay identidad de usuario; es la base de todo el sistema. |
| **2 — Alta** | Must | RF-11, RF-12, RF-13, RF-14 | El pronóstico es la funcionalidad central (razón de ser de la app). |
| **3 — Alta** | Must | RF-16, RF-17, RF-18, RF-21 | La puntuación y el ranking dan el valor competitivo; justifican pronosticar. |
| **4 — Media** | Should | RF-07, RF-08, RF-09, RF-10 | Los partidos alimentan el flujo de pronóstico; valor alto pero dependen de API externa. |
| **5 — Media** | Should | RF-15, RF-19, RF-20, RF-24 | Mejoran el seguimiento y la experiencia competitiva. |
| **6 — Media** | Should | RF-04, RF-25, RF-26, RF-28 | Gestión de cuenta y personalización; esperable pero no bloqueante. |
| **7 — Baja** | Could | RF-27 | Foto de perfil: mejora cosmética, no afecta el núcleo. |
| **8 — Baja** | Could | RF-22, RF-23 | Noticias: contenido complementario de valor agregado. |

### 4.2 Priorización de requisitos no funcionales

| Prioridad | RNF | Justificación |
|---|---|---|
| Alta | RNF-04, RNF-05, RNF-06 | Seguridad de credenciales y sesión: crítica por manejar datos personales. |
| Alta | RNF-03, RNF-11 | Rendimiento de red y modelo multi-dispositivo: habilitan el uso real. |
| Media | RNF-01, RNF-02, RNF-08, RNF-09 | Calidad de experiencia y mantenibilidad. |
| Baja | RNF-07, RNF-10, RNF-12 | Importantes pero no bloqueantes para el MVP. |

### 4.3 Resumen de requerimiento vs. requisito (ejemplos)

| Requerimiento (necesidad del cliente) | Requisito (especificación técnica) |
|---|---|
| "Quiero competir con otros aficionados." | RF-18, RF-19: ranking global ordenado por puntos y filtrable por período. |
| "Quiero ganar puntos cuando acierto." | RF-16, RF-17: regla de puntuación y resolución automática al finalizar. |
| "Quiero predecir el marcador de un partido." | RF-11, RF-12, RF-13: pronóstico de marcador exacto, único y con bloqueo por tiempo. |
| "No quiero que roben mi cuenta." | RNF-04, RNF-05, RNF-06: hash BCrypt, JWT y token cifrado en el dispositivo. |
| "Quiero usar mi cuenta en cualquier teléfono." | RNF-11, RF-21: datos centralizados en MySQL compartidos entre dispositivos. |
