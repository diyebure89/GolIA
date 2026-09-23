# Requirements Document

## Introduction

Esta funcionalidad añade una barra de navegación inferior (bottom navigation) a la pantalla principal `MainActivity` de la aplicación GolIA (predicciones de fútbol; Android, Java, Clean Architecture + MVVM + Hilt + Room). La pantalla replica la estructura del mockup de diseño ("Inicio" con encabezado GOL-IA, tarjeta de bienvenida con estadísticas y sección de partidos próximos), pero adopta la paleta de fondo azul/oscura del login (`@drawable/purple_backgraund` con `blue_dark`, `blue_start`, `blue_end`) en lugar del negro puro del mockup.

`MainActivity` alojará un contenedor de fragments y una `BottomNavigationView` de Material con cinco destinos: Inicio, Partidos, Noticias, Ranking y Perfil. Cada destino mostrará el fragment ya existente en el proyecto (`InicioFragment`, `PartidosFragment`, `NoticiasFragment`, `RankingFragment`, `PerfilFragment`).

El alcance de este spec es exclusivamente la navegación, la pantalla Home y el estilo visual alineado al login. Los fragments pueden mostrar datos estáticos/placeholder. La integración con APIs y datos reales queda para trabajo futuro; este spec solo debe dejar la arquitectura preparada para esa integración posterior (los fragments serán el punto donde luego entren ViewModels + repositorios).

Nota de integración: MainActivity es el host único de navegación tras el login; la clase HomeActivity y su layout activity_home.xml quedan obsoletos y LoginActivity deberá navegar a MainActivity tras un inicio de sesión exitoso (ajuste a realizar en implementación). Además, el inicio de sesión debe guardar el nombre del usuario en el estado de sesión local (PreferencesManager) para que la pantalla de inicio pueda mostrarlo sin acceder a datos; este es un ajuste sobre el flujo de login ya implementado.

### Fuera de Alcance

- Integración con APIs o fuentes de datos reales.
- Lógica de negocio de predicciones de fútbol.
- Contenido dinámico de los fragments Partidos, Noticias, Ranking y Perfil (mantienen su placeholder actual).
- Creación de ViewModels o repositorios por fragment (solo se deja la estructura preparada).
- La lectura de datos desde la base de datos o repositorios en la pantalla de inicio queda fuera de alcance; el nombre de usuario mostrado proviene exclusivamente del estado de sesión local guardado durante el login.

## Glossary

- **MainActivity**: Actividad principal que se muestra tras un login exitoso; aloja el Fragment_Container y la Bottom_Navigation; su layout es `activity_main.xml`.
- **Bottom_Navigation**: Componente `com.google.android.material.bottomnavigation.BottomNavigationView` que presenta los cinco destinos de navegación en la parte inferior de la pantalla.
- **Fragment_Container**: Contenedor de vista (por ejemplo `FragmentContainerView`) dentro del layout de `MainActivity` donde se muestra el fragment del destino seleccionado.
- **Destino**: Cada uno de los cinco ítems de la barra de navegación inferior: Inicio, Partidos, Noticias, Ranking, Perfil.
- **Estrategia_Fragmentos**: MainActivity mantiene las instancias de fragment añadidas al FragmentManager identificadas por etiqueta (tag) y alterna su visibilidad con show/hide, en lugar de reemplazarlas; el fragment visible se restaura por su etiqueta tras la recreación de la actividad.
- **InicioFragment**: Fragment del destino Inicio (`R.layout.fragment_inicio`); replica la estructura del mockup con datos placeholder.
- **PartidosFragment**: Fragment del destino Partidos (`R.layout.fragment_partidos`); muestra su placeholder actual.
- **NoticiasFragment**: Fragment del destino Noticias (`R.layout.fragment_noticias`); muestra su placeholder actual.
- **RankingFragment**: Fragment del destino Ranking (`R.layout.fragment_ranking`); muestra su placeholder actual.
- **PerfilFragment**: Fragment del destino Perfil (`R.layout.fragment_perfil`); muestra su placeholder actual.
- **Paleta_Login**: Conjunto de colores y fondo empleados por la pantalla de login: fondo `@drawable/purple_backgraund` y colores `blue_dark` (#0A0E1A), `blue_start` (#007BFF) y `blue_end` (#00C6FF), con texto en `white`.
- **Color_Activo**: Color aplicado al destino seleccionado en la barra de navegación (celeste/azul claro de la Paleta_Login, `blue_end`).
- **Color_Inactivo**: Color aplicado a los destinos no seleccionados en la barra de navegación (tono atenuado, por ejemplo `text_gray`).
- **Estado_Seleccionado**: Identificador del destino actualmente mostrado en el Fragment_Container.
- **Login_System**: Componente de autenticación existente que, tras un inicio de sesión exitoso, persiste el estado de sesión local (identificador y nombre del usuario) en PreferencesManager.

## Requirements

### Requirement 1: Estructura de Home con contenedor y barra de navegación

**User Story:** Como usuario autenticado, quiero que la pantalla principal tenga una barra de navegación inferior con cinco secciones, para poder moverme entre las áreas principales de la aplicación.

#### Acceptance Criteria

1. THE MainActivity SHALL mostrar un Fragment_Container y una Bottom_Navigation dentro de su layout.
2. THE Bottom_Navigation SHALL presentar exactamente cinco Destinos con las etiquetas Inicio, Partidos, Noticias, Ranking y Perfil.
3. THE Bottom_Navigation SHALL usar íconos vectoriales por Destino equivalentes a los del mockup (Inicio: casa; Partidos: calendario; Noticias: periódico; Ranking: trofeo; Perfil: persona); IF alguno de esos íconos no existe en el proyecto, THEN se creará como recurso vectorial.
4. THE Bottom_Navigation SHALL usar el recurso de cadena `@string/inicio`, `@string/partidos`, `@string/noticias`, `@string/ranking` y `@string/perfil` como etiquetas de los Destinos.

### Requirement 2: Navegación entre destinos

**User Story:** Como usuario autenticado, quiero pulsar cada ítem de la barra inferior, para ver la sección correspondiente en pantalla.

#### Acceptance Criteria

1. WHEN el usuario selecciona el Destino Inicio y el Destino Inicio no es el Estado_Seleccionado actual, THE MainActivity SHALL mostrar InicioFragment en el Fragment_Container como resultado directo de la selección.
2. WHEN el usuario selecciona el Destino Partidos y el Destino Partidos no es el Estado_Seleccionado actual, THE MainActivity SHALL mostrar PartidosFragment en el Fragment_Container como resultado directo de la selección.
3. WHEN el usuario selecciona el Destino Noticias y el Destino Noticias no es el Estado_Seleccionado actual, THE MainActivity SHALL mostrar NoticiasFragment en el Fragment_Container como resultado directo de la selección.
4. WHEN el usuario selecciona el Destino Ranking y el Destino Ranking no es el Estado_Seleccionado actual, THE MainActivity SHALL mostrar RankingFragment en el Fragment_Container como resultado directo de la selección.
5. WHEN el usuario selecciona el Destino Perfil y el Destino Perfil no es el Estado_Seleccionado actual, THE MainActivity SHALL mostrar PerfilFragment en el Fragment_Container como resultado directo de la selección.
6. WHEN el usuario selecciona un Destino, THE MainActivity SHALL actualizar el Estado_Seleccionado al Destino elegido, manteniendo exactamente un Destino seleccionado a la vez.
7. WHEN el usuario reselecciona el Destino que ya es el Estado_Seleccionado actual, THE MainActivity SHALL conservar el fragment visible sin recargarlo ni recrearlo.
8. WHEN el usuario regresa a un Destino visitado previamente durante la misma sesión de MainActivity, THE MainActivity SHALL reutilizar la instancia de fragment existente (patrón añadir/ocultar por etiqueta) en lugar de crear una nueva.

### Requirement 3: Destino inicial por defecto

**User Story:** Como usuario autenticado, quiero que al abrir la pantalla principal se muestre la sección Inicio, para ver de inmediato mi resumen y los partidos próximos.

#### Acceptance Criteria

1. WHEN MainActivity se crea por primera vez sin estado previo, THE MainActivity SHALL mostrar InicioFragment en el Fragment_Container.
2. WHEN MainActivity se crea por primera vez sin estado previo, THE Bottom_Navigation SHALL marcar el Destino Inicio como seleccionado.

### Requirement 4: Resaltado del destino seleccionado

**User Story:** Como usuario autenticado, quiero ver claramente qué sección está activa, para saber en qué parte de la aplicación me encuentro.

#### Acceptance Criteria

1. WHILE un Destino está seleccionado, THE Bottom_Navigation SHALL mostrar el ícono y la etiqueta de ese Destino con el Color_Activo.
2. WHILE un Destino no está seleccionado, THE Bottom_Navigation SHALL mostrar el ícono y la etiqueta de ese Destino con el Color_Inactivo.
3. THE Color_Activo SHALL ser un color celeste/azul claro de la Paleta_Login (`blue_end`).
4. WHEN cambia el Estado_Seleccionado, THE Bottom_Navigation SHALL aplicar el Color_Activo únicamente al nuevo Destino seleccionado.

### Requirement 5: Estilo visual alineado al login

**User Story:** Como usuario, quiero que la pantalla principal use la misma paleta azul/oscura del login, para tener una experiencia visual coherente en toda la aplicación.

#### Acceptance Criteria

1. THE MainActivity SHALL usar como fondo de pantalla la Paleta_Login (fondo azul/oscuro basado en `@drawable/purple_backgraund` o los colores `blue_dark`/`blue_start`/`blue_end`).
2. THE Fragment_Container SHALL definir el fondo con la Paleta_Login; THE InicioFragment (y los demás fragments) SHALL usar fondo transparente para heredar el del contenedor y evitar solapamientos.
3. THE Bottom_Navigation SHALL usar un color de fondo de la Paleta_Login (`blue_dark`).
4. THE MainActivity SHALL mostrar el texto principal en color `white` o en colores de la Paleta_Login para mantener contraste sobre el fondo oscuro.

### Requirement 6: Persistencia del destino ante cambios de configuración

**User Story:** Como usuario autenticado, quiero que al rotar la pantalla se conserve la sección en la que estaba, para no perder mi contexto de navegación.

#### Acceptance Criteria

1. WHEN MainActivity se recrea y existe un Estado_Seleccionado previo, THE MainActivity SHALL restaurar como visible el fragment correspondiente a ese Destino localizándolo por su etiqueta en el FragmentManager.
2. WHEN ocurre un cambio de configuración y existe un Estado_Seleccionado previo, THE Bottom_Navigation SHALL marcar como seleccionado el Destino correspondiente al Estado_Seleccionado previo.
3. THE MainActivity SHALL persistir el identificador del Destino seleccionado en onSaveInstanceState y restaurarlo tras la recreación; la preservación del estado de vista de cada fragment se delega al FragmentManager.
4. WHILE un Destino distinto de Inicio está seleccionado, WHEN ocurre un cambio de configuración, THE MainActivity SHALL conservar ese Destino como Estado_Seleccionado y SHALL NOT reiniciar la selección a Inicio.
5. WHEN MainActivity se recrea y no existe ningún Estado_Seleccionado previo, THE MainActivity SHALL seleccionar el Destino Inicio.

### Requirement 7: Contenido de la pantalla Inicio

**User Story:** Como usuario autenticado, quiero que la sección Inicio muestre la estructura del mockup con mi bienvenida, estadísticas y partidos próximos, para tener un resumen de mi actividad al abrir la aplicación.

#### Acceptance Criteria

1. THE InicioFragment SHALL mostrar un encabezado con el logo GOL-IA y un ícono de notificaciones; THE ícono de notificaciones SHALL ser interactivo (clickable) pero SHALL NOT ejecutar ninguna acción en este alcance.
2. THE InicioFragment SHALL mostrar en la tarjeta de bienvenida el nombre del usuario obtenido del estado de sesión local (PreferencesManager) guardado durante el inicio de sesión; IF no hay nombre disponible en la sesión, THEN THE InicioFragment SHALL mostrar un valor por defecto.
3. THE InicioFragment SHALL mostrar exactamente cuatro estadísticas con las etiquetas Pronóst., Aciertos, Puntos y Ranking, cada una con un valor visible (dato placeholder).
4. THE InicioFragment SHALL mostrar una sección titulada "Partidos próximos" con un enlace "Ver todos" visible dentro de la sección.
5. THE InicioFragment SHALL mostrar entre 1 y 10 tarjetas de partido, cada una con dos equipos y sus cuotas (datos placeholder).
6. THE InicioFragment SHALL poblar el contenido mostrado exclusivamente a partir de datos estáticos definidos en el propio fragment, sin realizar solicitudes de datos externas.
7. THE InicioFragment SHALL rediseñar su layout (`fragment_inicio.xml`) conforme al mockup, con la tarjeta de bienvenida, exactamente cuatro estadísticas independientes (Pronóst., Aciertos, Puntos, Ranking) y tarjetas de partido independientes con cuotas, reemplazando el contenido de texto concatenado actual.

### Requirement 8: Preparación para datos futuros

**User Story:** Como desarrollador, quiero que los fragments queden estructurados para una futura integración de APIs, para poder añadir ViewModels y repositorios sin rediseñar la navegación.

#### Acceptance Criteria

1. Cada Fragment (InicioFragment, PartidosFragment, NoticiasFragment, RankingFragment, PerfilFragment) SHALL centralizar el poblado de su UI en un único método (por ejemplo `bindPlaceholderData()`) invocado desde onViewCreated.
2. THE contrato de poblado de UI SHALL permitir que el reemplazo del contenido placeholder por datos provenientes de un ViewModel no altere la lógica de navegación de MainActivity.
3. THE MainActivity SHALL delegar el contenido de cada sección en su Fragment correspondiente, de modo que la lógica de datos futura resida en los fragments y no en la actividad.
4. Los cinco fragments SHALL compartir un contrato de UI común (por ejemplo, una clase base o interfaz con un método de poblado de datos) para homogeneizar la futura conexión de ViewModels.

### Requirement 9: Comportamiento del botón atrás y reselección de pestaña

**User Story:** Como usuario autenticado, quiero un comportamiento predecible al reseleccionar una pestaña o pulsar atrás, para navegar con comodidad según las buenas prácticas de navegación inferior.

#### Acceptance Criteria

1. WHEN el usuario reselecciona el Destino que ya es el Estado_Seleccionado actual, THE MainActivity SHALL mantener el fragment actual sin crear una nueva instancia y conservando su estado de vista.
2. THE MainActivity SHALL implementar el manejo del botón atrás mediante OnBackPressedCallback (no `onBackPressed` deprecado). En este alcance los fragments no mantienen back stack interno propio.
3. WHILE un Destino distinto de Inicio es el Estado_Seleccionado, WHEN el usuario pulsa el botón atrás, THE MainActivity SHALL seleccionar el Destino Inicio y mostrar InicioFragment.
4. WHILE el Destino Inicio es el Estado_Seleccionado, WHEN el usuario pulsa el botón atrás, THE MainActivity SHALL delegar el comportamiento de salida predeterminado del sistema.

### Requirement 10: Accesibilidad básica

**User Story:** Como usuario que utiliza tecnologías de asistencia, quiero que los ítems de navegación tengan etiquetas y buen contraste, para poder identificar y usar cada sección.

#### Acceptance Criteria

1. THE Bottom_Navigation SHALL proporcionar una etiqueta de texto descriptiva para cada Destino.
2. THE Bottom_Navigation SHALL aplicar el tint activo/inactivo mediante un ColorStateList (`itemIconTint` e `itemTextColor`) y exponer una descripción accesible (contentDescription o etiqueta de menú) para cada Destino.
3. THE ratio de contraste entre Color_Activo y el color de fondo de la Bottom_Navigation SHALL ser mayor o igual a 3:1 para elementos no textuales, y el texto de la etiqueta del Destino seleccionado SHALL tener un ratio de contraste mayor o igual a 4.5:1 (WCAG AA).

### Requirement 11: Persistencia del nombre de usuario en la sesión

**User Story:** Como usuario autenticado, quiero que la pantalla de inicio salude con mi nombre, para tener una experiencia personalizada tras iniciar sesión.

#### Acceptance Criteria

1. WHEN un inicio de sesión es exitoso, THE Login_System SHALL persistir en el estado de sesión local (PreferencesManager) el nombre del usuario autenticado, además del identificador de usuario ya almacenado.
2. THE InicioFragment SHALL leer el nombre del usuario únicamente desde el estado de sesión local (PreferencesManager), sin acceder a la base de datos ni a repositorios, para respetar el alcance de esta funcionalidad.
3. IF el estado de sesión local no contiene un nombre de usuario, THEN THE InicioFragment SHALL mostrar un valor por defecto.
