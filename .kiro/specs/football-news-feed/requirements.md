# Requirements Document

## Introduction

Esta funcionalidad dota de contenido real al fragment de Noticias (`NoticiasFragment`, layout `fragment_noticias.xml`, actualmente un placeholder con un `TextView`) de la app GolIA, una aplicación Android en Java construida con Clean Architecture + MVVM + Hilt + Room. La pantalla debe mostrar noticias de fútbol filtradas por liga mediante chips, reutilizando exactamente el mismo conjunto de ligas (Competiciones_Objetivo) que ya define y consume el fragment de Partidos, de modo que ambos fragments permanezcan sincronizados desde una única fuente de verdad.

Las noticias provienen de la API gratuita NewsData.io. Se consulta por palabras clave derivadas de cada liga (nombre de la liga en español/inglés y, opcionalmente, equipos conocidos de esa liga), con idioma español (`lang=es`) y categoría deportes. El chip "Todos" muestra un feed agregado de fútbol. El proveedor queda detrás de una interfaz de dominio (`NewsRepository`) para poder cambiar de proveedor sin afectar a la capa de presentación.

La pantalla observa estado desde un ViewModel `@HiltViewModel` que consume el repositorio de dominio mediante casos de uso; el fragment no llama directamente a la API. Se reutilizan los patrones ya establecidos por la feature `matches-live-fixtures`: gestión de presupuesto de peticiones (al estilo `RequestBudgetManager`), planificador de refresco (al estilo `LiveRefreshScheduler`), cadena de mappers DTO → dominio → UI, resultado tipado `domain/common/Result`, ejecución de E/S con `@IoExecutor`, y `Adapter` con `DiffUtil`. La API key de NewsData.io se lee de `local.properties` expuesta vía `BuildConfig`, replicando el patrón de `API_FOOTBALL_KEY` con un interceptor OkHttp, y no se versiona.

La UI, basada en el mockup, presenta el título "Noticias Deportivas", un icono de búsqueda, una fila horizontal de chips de liga y una lista de tarjetas de noticia con imagen de fondo, badge de categoría/liga y texto, con tema oscuro coherente con el resto de la app. Debe manejar los estados de carga, contenido, vacío, error y sin conexión, e incluir búsqueda por texto sobre las noticias.

### Fuera de alcance (explícito)

- El contenido de otros fragments (Inicio, Partidos, Perfil, Ranking, Predicciones).
- La lógica de predicciones y apuestas.
- La autenticación de usuario.
- El artículo completo: el plan gratuito de NewsData.io no entrega el cuerpo completo del artículo; abrir la noticia completa se delega al navegador/URL externo.
- El badge ilustrativo "NBA" del mockup es un ejemplo visual; este spec cubre únicamente noticias de fútbol de las Competiciones_Objetivo.

### Riesgos y compromisos a reflejar

- El plan gratuito de NewsData.io ofrece aproximadamente 200 créditos/día (hasta ~10 artículos por crédito) y limita la consulta por palabra clave (`q`) a 100 caracteres. Esto obliga a caché local agresiva en Room, refresco controlado e intervalo mínimo entre refrescos. (Contenido reformulado por cumplimiento de licencia a partir de la documentación del plan gratuito de NewsData.io.)
- La actualización es casi en tiempo real, con frescura de hasta 30 minutos por liga: refresco al abrir el fragment, pull-to-refresh manual e intervalo mínimo anti-abuso de cuota de 30 minutos por chip, apoyado en caché.
- Se descartó NewsAPI.org porque su plan gratuito solo funciona en localhost y aplica un retraso de ~24h en los artículos. (Contenido reformulado por cumplimiento de licencia.)
- La API key no debe versionarse; se lee de `local.properties` vía `BuildConfig` y se adjunta mediante interceptor/parámetro de consulta.
- El conjunto de ligas NO debe duplicarse: debe reutilizar la misma fuente de verdad que Partidos (actualmente `FixtureMapper.TARGET_LEAGUE_IDS` y sus constantes de ID de liga), para que agregar o quitar una liga se refleje en ambos fragments.

## Glossary

- **App**: La aplicación Android GolIA en su conjunto.
- **Pantalla_Noticias**: La interfaz gestionada por `NoticiasFragment` (`R.layout.fragment_noticias`) que muestra el título, el icono de búsqueda, la fila de chips de liga y la lista de tarjetas de noticia.
- **ViewModel_Noticias**: El `@HiltViewModel` que provee el estado observable de la Pantalla_Noticias y coordina los casos de uso.
- **Repositorio_Noticias**: La implementación de `NewsRepository` (dominio) que orquesta datos remotos (NewsData.io) y locales (Room), oculta el proveedor concreto y expone resultados tipados.
- **Proveedor_Noticias**: El proveedor externo de noticias configurado; en este spec, NewsData.io (plan gratuito).
- **Servicio_API_Noticias**: El cliente Retrofit que consume los endpoints del Proveedor_Noticias.
- **Cache_Noticias**: El almacenamiento local en Room (`NewsArticleDao`, entidad de artículo, `GolIADatabase`) de artículos de noticia y sus metadatos de liga/consulta. La identidad de cada Articulo_Noticia es un hash estable de su URL canónica (sin parámetros de tracking), y la relación artículo–liga es N:M mediante una tabla de cross-reference.
- **Adaptador_Noticias**: El `RecyclerView.Adapter` que renderiza las tarjetas de noticia usando `DiffUtil`.
- **Articulo_Noticia**: El objeto de dominio que representa una noticia, con los campos disponibles en el plan gratuito: título, descripción, URL de imagen, nombre de la fuente, URL del artículo y fecha de publicación.
- **Competiciones_Objetivo**: El conjunto exacto de ligas cuyas noticias se muestran, idéntico al usado por el fragment de Partidos y tomado de la misma fuente de verdad (`FixtureMapper.TARGET_LEAGUE_IDS`): ligas domésticas (39 Premier League, 140 LaLiga/Primera División, 135 Serie A, 78 Bundesliga, 61 Ligue 1, 239 Colombia Primera A), competiciones de clubes (2 UEFA Champions League) y competiciones de selecciones (5 UEFA Nations League y la Clasificación al Mundial por confederación: 32 Europa, 34 Sudamérica/Conmebol, 29 África, 30 Asia, 31 CONCACAF, 33 Oceanía, 37 Repechaje Intercontinental).
- **Chip_Liga**: Cada uno de los chips del `ChipGroup` de la Pantalla_Noticias; incluye el Chip_Todos, un chip por cada Competición_Objetivo no perteneciente a la clasificación al Mundial, y un único Chip_Eliminatorias que agrupa los siete identificadores de clasificación al Mundial.
- **Chip_Todos**: El chip "Todos" que muestra un feed agregado de noticias de fútbol.
- **Terminos_Consulta_Liga**: El conjunto de palabras clave asociadas a una Competición_Objetivo (nombre de la liga en español/inglés y, opcionalmente, equipos conocidos) usadas para construir la consulta `q` al Proveedor_Noticias.
- **LeagueNewsQuery**: La fuente de verdad complementaria que asocia cada identificador de liga (`LEAGUE_*`) con una lista ordenada por prioridad de Terminos_Consulta_Liga (en este spec, el nombre de la liga). Es el puente entre el identificador numérico usado por Partidos y la consulta textual `q` del Proveedor_Noticias. Debe definir al menos un término no vacío por cada `LEAGUE_*` id.
- **Chip_Eliminatorias**: Un único Chip_Liga que agrupa todas las competiciones de clasificación al Mundial (identificadores 32, 34, 29, 30, 31, 33, 37) bajo la etiqueta "Eliminatorias", en lugar de un chip por confederación.
- **NewsDataQueryBuilder**: El componente de la capa de datos remota que traduce la lista priorizada de Terminos_Consulta_Liga a la sintaxis de consulta concreta del Proveedor_Noticias (operador OR, comillas de frase, codificación). Aísla la sintaxis del proveedor de la capa de dominio.
- **API_Key_Noticias**: La clave de autenticación de NewsData.io, leída de `local.properties` y expuesta vía `BuildConfig`.
- **Presupuesto_Creditos**: El contador diario persistente de créditos/peticiones consumidos contra el Proveedor_Noticias, con límite de 200 créditos/día y reset a las 00:00 UTC (alineado con el proveedor). Se reserva un margen operativo usando como máximo 180 de los 200 créditos diarios, usado para respetar la cuota gratuita.
- **Intervalo_Minimo_Refresco**: El intervalo mínimo entre refrescos (apertura de pantalla, pull-to-refresh) que descarta las solicitudes que lleguen antes de cumplirse dicho intervalo desde la última petición. Valor: 30 minutos (1800 segundos). Es POR (Chip_Liga, consulta), no global: cada liga tiene su propia marca de tiempo `lastFetchedAt` en la Cache_Noticias.
- **Estado_UI**: El estado observable de la Pantalla_Noticias, modelado como un objeto compuesto (data class) en lugar de un enum plano, con los campos: `items` (List<Articulo_Noticia> a mostrar), `loading` (boolean), `banner` (uno de: NINGUNO, OFFLINE, QUOTA, ERROR) y `empty` (uno de: NINGUNO, LEAGUE, SEARCH). Permite representar "contenido + banner" simultáneamente (por ejemplo, mostrar items cacheados junto a un aviso OFFLINE o QUOTA sin ocultar el contenido).
- **Zona_Local**: La zona horaria configurada en el dispositivo del usuario.

## Requirements

### Requisito 1: Reutilización de la fuente de verdad de ligas

**Historia de Usuario:** Como usuario, quiero que los filtros de liga de Noticias sean exactamente los mismos que los de Partidos, para tener una experiencia coherente entre ambas pantallas.

#### Criterios de Aceptación

1. THE Pantalla_Noticias SHALL derivar el conjunto de Chip_Liga de la misma fuente de verdad de ligas que consume el fragment de Partidos (FixtureMapper.TARGET_LEAGUE_IDS y sus constantes LEAGUE_*), sin declarar una lista de ligas independiente.
2. WHEN la Pantalla_Noticias renderiza la fila de chips, THE Pantalla_Noticias SHALL mostrar exactamente el Chip_Todos, un Chip_Liga por cada identificador de FixtureMapper.TARGET_LEAGUE_IDS que NO pertenezca al conjunto de clasificación al Mundial (32, 34, 29, 30, 31, 33, 37), y un único Chip_Eliminatorias que represente a ese conjunto de clasificación al Mundial, de modo que la cantidad total de Chip_Liga sea igual a (número de identificadores no-eliminatorias de FixtureMapper.TARGET_LEAGUE_IDS) + 1 (Chip_Eliminatorias) + 1 (Chip_Todos), sin chips de ligas ajenas al conjunto.
3. WHEN se añade o elimina un identificador no perteneciente a la clasificación al Mundial en FixtureMapper.TARGET_LEAGUE_IDS, THE Pantalla_Noticias SHALL reflejar en el siguiente renderizado un aumento o reducción de exactamente un Chip_Liga por identificador, sin requerir una lista de ligas independiente; los siete identificadores de clasificación al Mundial (32, 34, 29, 30, 31, 33, 37) SHALL representarse siempre mediante el único Chip_Eliminatorias.
4. THE Repositorio_Noticias SHALL identificar cada Competición_Objetivo por el mismo identificador numérico entero (API-Football league id) definido en las constantes LEAGUE_* usadas por el fragment de Partidos.
5. IF FixtureMapper.TARGET_LEAGUE_IDS está vacío o no puede resolverse, THEN THE Pantalla_Noticias SHALL mostrar únicamente el Chip_Todos sin mostrar un error visible y SHALL permanecer operativa.
6. THE App SHALL definir en LeagueNewsQuery al menos un término de consulta no vacío para cada identificador `LEAGUE_*` incluido en `FixtureMapper.TARGET_LEAGUE_IDS`.

### Requisito 2: Filtro por chips de liga

**Historia de Usuario:** Como usuario, quiero filtrar las noticias por liga mediante chips, para leer solo las noticias de las competiciones que me interesan.

#### Criterios de Aceptación

1. THE Pantalla_Noticias SHALL mostrar una fila horizontal desplazable de Chip_Liga con el Chip_Todos en primera posición.
2. THE ChipGroup SHALL permitir la selección de un único Chip_Liga a la vez.
3. WHEN la Pantalla_Noticias se abre, THE Pantalla_Noticias SHALL seleccionar por defecto el Chip_Todos.
4. WHEN el usuario selecciona el Chip_Todos, THE ViewModel_Noticias SHALL mostrar un feed agregado de noticias de fútbol de todas las Competición_Objetivo disponibles.
5. WHEN el usuario selecciona un Chip_Liga distinto del Chip_Todos, THE ViewModel_Noticias SHALL reemplazar el contenido del feed mostrando únicamente las noticias asociadas a los Terminos_Consulta_Liga de esa Competición_Objetivo y reposicionar la lista en el primer elemento.
6. WHERE un Chip_Liga está seleccionado, THE Pantalla_Noticias SHALL mostrar ese Chip_Liga con un estado visual seleccionado distinto (color de fondo, borde o marca de selección) del estado de los Chip_Liga no seleccionados, de modo que dos observadores independientes identifiquen el mismo chip como seleccionado.
7. WHILE la consulta de noticias del Chip_Liga seleccionado está en curso, THE Pantalla_Noticias SHALL mostrar un indicador de carga y completar la operación en un máximo de 10 segundos.
8. IF la consulta de noticias del Chip_Liga seleccionado no devuelve ningún resultado, THEN THE Pantalla_Noticias SHALL mostrar un estado vacío indicando que no hay noticias disponibles para esa liga y conservar el chip seleccionado.
9. IF la consulta de noticias falla o supera el límite de 10 segundos, THEN THE Pantalla_Noticias SHALL mostrar un mensaje de error indicando el fallo de carga, conservar el Chip_Liga seleccionado y ofrecer la posibilidad de reintentar sin perder la selección actual.

### Requisito 3: Consulta al proveedor por palabras clave de liga

**Historia de Usuario:** Como usuario, quiero que la app busque noticias relevantes a cada liga, para recibir contenido pertinente al filtro seleccionado.

#### Criterios de Aceptación

1. WHEN el usuario selecciona un Chip_Liga distinto del Chip_Todos, THE Repositorio_Noticias SHALL obtener la lista priorizada de Terminos_Consulta_Liga de LeagueNewsQuery para esa Competición_Objetivo y entregarla al NewsDataQueryBuilder sin construir la sintaxis del proveedor en la capa de dominio.
2. THE NewsDataQueryBuilder SHALL construir la cadena `q` concatenando los términos en orden de prioridad descendente con el operador del proveedor, midiendo la longitud en caracteres del parámetro `q` ANTES de la codificación URL, y WHILE la cadena excede 100 caracteres SHALL eliminar el término de menor prioridad (ante empate, el de mayor índice en la lista) hasta que la longitud sea menor o igual a 100, conservando siempre al menos el término de mayor prioridad. La construcción de la sintaxis (operador OR, comillas de frase, codificación) reside en la capa de datos remota (NewsDataQueryBuilder), no en el Repositorio_Noticias de dominio, para cumplir R4.
3. IF los Terminos_Consulta_Liga de la Competición_Objetivo están vacíos o nulos, THEN THE Repositorio_Noticias SHALL construir la consulta de feed agregado de fútbol definida para el Chip_Todos.
4. WHEN el Servicio_API_Noticias consulta al Proveedor_Noticias, THE Servicio_API_Noticias SHALL enviar el parámetro de idioma lang=es y la categoría de deportes.
5. WHEN el usuario selecciona el Chip_Todos, THE Repositorio_Noticias SHALL construir una consulta de feed agregado de fútbol con una longitud máxima de 100 caracteres para la consulta q.
6. IF el Proveedor_Noticias no responde dentro de 10 segundos o responde con un fallo, THEN THE Repositorio_Noticias SHALL devolver un resultado de error indicando la falla de la consulta de noticias y SHALL conservar el listado de noticias previamente cargado sin modificarlo.
7. WHEN el Proveedor_Noticias responde con artículos, THE Repositorio_Noticias SHALL mapear cada artículo a un Articulo_Noticia de dominio conservando título, descripción, URL de imagen, nombre de la fuente, URL del artículo y fecha de publicación.
8. IF un artículo de la respuesta del Proveedor_Noticias carece de título o de URL del artículo, THEN THE Repositorio_Noticias SHALL descartar ese artículo y continuar el mapeo de los artículos restantes.
9. WHEN el Proveedor_Noticias responde sin artículos, THE Repositorio_Noticias SHALL devolver un listado de noticias vacío sin indicar error.
10. THE Repositorio_Noticias SHALL ordenar los Articulo_Noticia por fecha de publicación descendente.
11. THE mapper de datos SHALL normalizar la fecha de publicación del DTO del proveedor a epoch UTC; IF la fecha no es parseable, THEN THE mapper SHALL descartar ese artículo.

### Requisito 4: Abstracción del proveedor de noticias

**Historia de Usuario:** Como desarrollador, quiero que el proveedor de noticias esté detrás de una interfaz de dominio, para poder cambiar de proveedor sin modificar la capa de presentación.

#### Criterios de Aceptación

1. WHEN la Pantalla_Noticias solicita la lista de noticias, THE ViewModel_Noticias SHALL obtener las noticias exclusivamente mediante casos de uso del dominio que consumen la interfaz NewsRepository, sin referenciar en tiempo de compilación al Servicio_API_Noticias ni a ninguna clase del proveedor concreto.
2. THE Pantalla_Noticias SHALL NOT invocar directamente el Servicio_API_Noticias ni el Repositorio_Noticias, comunicándose únicamente con el ViewModel_Noticias.
3. WHEN el Servicio_API_Noticias recibe una respuesta correcta del Proveedor_Noticias, THE mapper de datos SHALL transformar cada DTO del proveedor en un objeto de dominio Articulo_Noticia, de modo que las clases DTO específicas del proveedor no sean referenciadas fuera de las capas de datos remoto y mapper.
4. THE interfaz de dominio NewsRepository SHALL exponer en su contrato únicamente objetos de dominio (Articulo_Noticia) y tipos estándar del lenguaje, y SHALL NOT exponer tipos específicos de NewsData.io (DTOs del proveedor).
5. IF el mapper recibe un DTO del proveedor con campos obligatorios de Articulo_Noticia ausentes o nulos, THEN THE mapper de datos SHALL descartar ese elemento de la lista resultante y continuar el mapeo de los elementos restantes, sin propagar tipos del proveedor a las capas superiores.
6. THE construcción de la sintaxis de consulta específica del proveedor (operador OR, comillas, codificación) SHALL residir en el NewsDataQueryBuilder de la capa de datos remota, y THE Repositorio_Noticias de dominio SHALL NOT contener sintaxis específica de NewsData.io.

### Requisito 5: Seguridad de la API Key de noticias

**Historia de Usuario:** Como desarrollador, quiero mantener la API_Key_Noticias fuera del control de versiones, para evitar exponer credenciales en el repositorio.

#### Criterios de Aceptación

1. THE App SHALL leer la API_Key_Noticias desde local.properties y exponerla en tiempo de compilación vía BuildConfig, con el mismo mecanismo de lectura que API_FOOTBALL_KEY.
2. THE valor literal de la API_Key_Noticias SHALL NOT aparecer en ningún archivo bajo control de versiones.
3. WHEN el Servicio_API_Noticias realiza una petición al Proveedor_Noticias, THE Servicio_API_Noticias SHALL adjuntar la API_Key_Noticias a esa petición mediante un interceptor OkHttp o un parámetro de consulta.
4. THE Servicio_API_Noticias SHALL adjuntar la API_Key_Noticias a toda petición al Proveedor_Noticias sin requerir que la clave se pase como argumento por método.
5. IF la API_Key_Noticias no está configurada (ausente o cadena vacía) al iniciar la Pantalla_Noticias, THEN THE ViewModel_Noticias SHALL exponer un Estado_UI con `banner=ERROR` que indique la falta de configuración de la clave en un máximo de 1 segundo y SHALL NOT realizar peticiones al Proveedor_Noticias.

### Requisito 6: Refresco casi en tiempo real y política de cuota

**Historia de Usuario:** Como usuario, quiero que las noticias se mantengan actualizadas (con frescura de hasta 30 minutos por liga) sin que la app agote la cuota diaria, para tener contenido fresco de forma sostenible.

#### Criterios de Aceptación

1. WHEN la Pantalla_Noticias se abre, THE Repositorio_Noticias SHALL refrescar las noticias del Chip_Liga seleccionado desde el Proveedor_Noticias, respetando el Intervalo_Minimo_Refresco de 30 minutos (1800 segundos) evaluado por (Chip_Liga, consulta) contra la marca `lastFetchedAt` de esa liga en la Cache_Noticias; WHEN el Chip_Liga seleccionado nunca ha sido refrescado (sin `lastFetchedAt`), THE Repositorio_Noticias SHALL consultar al Proveedor_Noticias si hay cuota disponible; WHEN el Chip_Liga seleccionado tiene `lastFetchedAt` dentro de los últimos 30 minutos, THE Repositorio_Noticias SHALL servir desde la Cache_Noticias sin consultar.
2. WHEN el usuario realiza pull-to-refresh, THE Repositorio_Noticias SHALL refrescar las noticias del Chip_Liga seleccionado desde el Proveedor_Noticias, respetando el Intervalo_Minimo_Refresco de 30 minutos (1800 segundos) evaluado por (Chip_Liga, consulta) contra la marca `lastFetchedAt` de esa liga en la Cache_Noticias.
3. WHILE cualquier fuente de refresco (apertura de pantalla o pull-to-refresh) solicita datos, THE ViewModel_Noticias SHALL aplicar el Intervalo_Minimo_Refresco por (Chip_Liga, consulta) y no global, descartando las solicitudes que lleguen antes de cumplirse 1800 segundos desde la última petición al Proveedor_Noticias para esa misma (Chip_Liga, consulta).
4. WHEN el Repositorio_Noticias realiza una petición al Proveedor_Noticias, THE Repositorio_Noticias SHALL incrementar el Presupuesto_Creditos, que es un contador diario persistente con límite operativo de 180 créditos/día (margen sobre los 200 del proveedor) y reset a las 00:00 UTC.
5. IF el Presupuesto_Creditos ha alcanzado los 180 créditos operativos del día, THEN THE ViewModel_Noticias SHALL degradar a modo solo-caché mostrando los datos de la Cache_Noticias junto con un aviso de que se ha alcanzado el límite diario, y SHALL NOT realizar más peticiones al Proveedor_Noticias hasta el reset de las 00:00 UTC.
6. THE Repositorio_Noticias SHALL persistir los artículos obtenidos en la Cache_Noticias asociándolos al Chip_Liga o al Chip_Todos correspondiente.
7. WHEN la Pantalla_Noticias se abre y la Cache_Noticias contiene datos del Chip_Liga seleccionado, THE Pantalla_Noticias SHALL mostrar los datos cacheados mientras se completa el refresco remoto.
8. IF una petición de refresco al Proveedor_Noticias falla, THEN THE Repositorio_Noticias SHALL conservar los datos de la Cache_Noticias sin descartarlos y SHALL devolver un resultado de error para que la Pantalla_Noticias pueda avisar del fallo.
9. THE chequeo-e-incremento del Presupuesto_Creditos y la evaluación del Intervalo_Minimo_Refresco SHALL ejecutarse como una sección crítica atómica, de modo que refrescos concurrentes (apertura de pantalla y pull-to-refresh simultáneos) no produzcan peticiones duplicadas ni doble consumo de crédito.
10. IF el Proveedor_Noticias responde indicando cuota agotada (por ejemplo HTTP 429), THEN THE App SHALL entrar en modo solo-caché hasta el próximo reset con independencia del contador local del Presupuesto_Creditos.
11. THE Cache_Noticias SHALL conservar como máximo 50 Articulo_Noticia por Chip_Liga y purgar los más antiguos por fecha de publicación cuando se supere ese límite.

### Requisito 7: Contenido de la tarjeta de noticia

**Historia de Usuario:** Como usuario, quiero ver la información clave de cada noticia en una tarjeta, para decidir de un vistazo si me interesa leerla.

#### Criterios de Aceptación

1. WHEN el Adaptador_Noticias renderiza un Articulo_Noticia, THE Adaptador_Noticias SHALL mostrar el título truncado con puntos suspensivos a un máximo de 2 líneas, la descripción truncada con puntos suspensivos a un máximo de 3 líneas, el nombre de la fuente truncado con puntos suspensivos a un máximo de 1 línea, y la fecha de publicación.
2. IF el título, la descripción o el nombre de la fuente de un Articulo_Noticia está ausente o vacío, THEN THE Adaptador_Noticias SHALL ocultar el campo correspondiente sin dejar espacio en blanco visible y renderizar los campos restantes de la tarjeta. La descripción y la imagen son campos opcionales del plan gratuito que pueden venir nulos y deben tratarse según los criterios 7.2 y 7.4.
3. WHEN el Adaptador_Noticias renderiza un Articulo_Noticia, THE Adaptador_Noticias SHALL cargar la imagen de fondo desde la URL de imagen del artículo.
4. IF la URL de imagen de un Articulo_Noticia está ausente, no se puede cargar, o la carga no finaliza dentro de 10 segundos, THEN THE Adaptador_Noticias SHALL mostrar una imagen placeholder en su lugar.
5. WHEN el Adaptador_Noticias renderiza un Articulo_Noticia, THE Adaptador_Noticias SHALL mostrar un badge de categoría/liga que identifique la Competición_Objetivo asociada, o una etiqueta genérica de fútbol cuando el artículo pertenezca al feed del Chip_Todos.
6. WHEN el Adaptador_Noticias presenta la fecha de publicación, THE Adaptador_Noticias SHALL formatearla usando la fecha corta y la hora corta de la Zona_Local, incluyendo día, mes, año y hora según el formato regional del dispositivo.
7. WHEN el usuario selecciona un Articulo_Noticia, THE Pantalla_Noticias SHALL abrir la URL del artículo en el navegador externo del dispositivo.
8. IF no existe una aplicación de navegador externo capaz de abrir la URL del artículo, THEN THE Pantalla_Noticias SHALL mantener visible la Pantalla_Noticias sin cambios y mostrar un mensaje indicando que no se pudo abrir el enlace.

### Requisito 8: Búsqueda de noticias por texto

**Historia de Usuario:** Como usuario, quiero buscar dentro de las noticias mostradas por texto, para localizar rápidamente un tema concreto.

#### Criterios de Aceptación

1. WHEN el usuario activa el icono de búsqueda del encabezado, THE Pantalla_Noticias SHALL mostrar un campo de entrada de texto para la búsqueda y trasladar el foco de entrada a dicho campo.
2. WHEN el usuario introduce texto no vacío en el campo de búsqueda, THE ViewModel_Noticias SHALL filtrar la lista del Chip_Liga seleccionado conservando únicamente los Articulo_Noticia cuyo título o descripción contengan, como subcadena, el texto introducido tras la normalización.
3. WHEN el usuario introduce o modifica el texto de búsqueda, THE ViewModel_Noticias SHALL aplicar un debounce de 300 milisegundos antes de recalcular la lista filtrada, cancelando el cálculo pendiente anterior.
4. WHEN el ViewModel_Noticias compara el texto de búsqueda con el título y la descripción, THE ViewModel_Noticias SHALL normalizar ambos lados sin distinguir mayúsculas y sin distinguir acentos ni diacríticos.
5. IF el texto de búsqueda está vacío o contiene solo espacios en blanco, THEN THE ViewModel_Noticias SHALL mostrar la lista completa correspondiente al Chip_Liga seleccionado sin aplicar filtro de texto.
6. THE ViewModel_Noticias SHALL considerar como máximo los primeros 256 caracteres del texto de búsqueda, ignorando el exceso.
7. WHEN el texto de búsqueda no coincide con ningún Articulo_Noticia de la lista del Chip_Liga seleccionado, THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` vacío y `empty=SEARCH`, mostrando un estado vacío específico de búsqueda con un mensaje de "sin resultados de búsqueda", distinto del estado vacío de liga (`empty=LEAGUE`).
8. WHEN el usuario limpia el texto de búsqueda o cierra el campo de búsqueda, THE ViewModel_Noticias SHALL restaurar la lista correspondiente al Chip_Liga seleccionado.

### Requisito 9: Estados de la interfaz

**Historia de Usuario:** Como usuario, quiero recibir retroalimentación clara mientras cargan las noticias o cuando algo falla, para entender qué está ocurriendo en la pantalla.

#### Criterios de Aceptación

1. WHILE el Repositorio_Noticias obtiene datos y `items` está vacío (aún no hay contenido cacheado para mostrar), THE Pantalla_Noticias SHALL exponer un Estado_UI con `loading=true`, `items` vacío, `empty=NINGUNO` y `banner=NINGUNO`, mostrando el indicador de carga y ocultando la lista, el estado vacío y el banner de error.
2. WHEN el Repositorio_Noticias entrega un resultado con éxito para el Chip_Liga seleccionado, THE Pantalla_Noticias SHALL fijar `loading=false` en el Estado_UI en un máximo de 300 milisegundos tras recibir el resultado.
3. WHEN el Chip_Liga seleccionado no tiene noticias que mostrar tras completar la carga, THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` vacío, `loading=false` y `empty=LEAGUE`, mostrando el estado vacío de liga y ocultando la lista y el indicador de carga.
4. IF ocurre un fallo del Proveedor_Noticias o de red y `items` está vacío (no hay datos cacheados que mostrar), THEN THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` vacío, `loading=false` y `banner=ERROR`, con un mensaje que identifique el fallo y una acción de reintentar visible.
5. WHEN el usuario activa la acción de reintentar, THE ViewModel_Noticias SHALL volver a solicitar el refresco de noticias del Chip_Liga seleccionado y fijar `loading=true` en el Estado_UI mientras la solicitud está en curso.
6. WHILE una solicitud de refresco del Chip_Liga seleccionado está en curso (`loading=true`), IF el usuario activa nuevamente la acción de reintentar, THEN THE ViewModel_Noticias SHALL ignorar la nueva activación sin iniciar una solicitud adicional.

### Requisito 10: Comportamiento sin conexión y manejo de errores

**Historia de Usuario:** Como usuario, quiero seguir viendo noticias aunque no tenga conexión, para consultar el último contenido disponible cuando la red o el proveedor fallen.

#### Criterios de Aceptación

1. IF no hay conexión de red al abrir la Pantalla_Noticias y la Cache_Noticias contiene datos, THEN THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` no vacío (datos de la Cache_Noticias) y `banner=OFFLINE`, mostrando el contenido cacheado junto con el aviso persistente sin ocultar el contenido.
2. IF no hay conexión de red al abrir la Pantalla_Noticias y la Cache_Noticias está vacía, THEN THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` vacío y `banner=OFFLINE`, con un mensaje que indique la ausencia de conexión y una acción de reintentar visible.
3. IF el Proveedor_Noticias responde con un error o la cuota diaria se ha agotado y la Cache_Noticias contiene datos, THEN THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` no vacío (datos de la Cache_Noticias) y `banner=QUOTA` o `banner=ERROR` según el motivo, mostrando el contenido cacheado junto con el aviso sin descartar los datos mostrados.
4. IF el Proveedor_Noticias responde con un error o la cuota diaria se ha agotado y la Cache_Noticias está vacía, THEN THE Pantalla_Noticias SHALL exponer un Estado_UI con `items` vacío y `banner=QUOTA` o `banner=ERROR` según el motivo, con un mensaje que lo identifique y una acción de reintentar visible.
5. WHEN se restablece la red y el usuario solicita un refresco, THE Repositorio_Noticias SHALL intentar actualizar los datos desde el Proveedor_Noticias.
6. WHEN un intento de refresco desde el Proveedor_Noticias finaliza, THE Pantalla_Noticias SHALL actualizar la lista con los datos obtenidos si el intento tuvo éxito, o conservar los datos de la Cache_Noticias mostrados y presentar un mensaje que identifique el fallo si el intento no tuvo éxito.

### Requisito 11: Arquitectura y preparación de la pantalla

**Historia de Usuario:** Como desarrollador, quiero que la Pantalla_Noticias siga la arquitectura del proyecto, para mantener la coherencia, la testabilidad y la mantenibilidad.

#### Criterios de Aceptación

1. WHEN el ViewModel_Noticias actualiza su Estado_UI, THE Pantalla_Noticias SHALL observar dicho estado mediante LiveData y SHALL renderizar el nuevo estado sin retener referencias que sobrevivan al ciclo de vida de la vista.
2. THE ViewModel_Noticias SHALL estar anotado con @HiltViewModel y SHALL obtener los datos de noticias exclusivamente a través de casos de uso del dominio que consuman NewsRepository, sin invocar directamente el Repositorio_Noticias ni fuentes de datos.
3. WHEN el ViewModel_Noticias entrega una nueva lista de noticias, THE Pantalla_Noticias SHALL usar el Adaptador_Noticias con DiffUtil para calcular las diferencias y SHALL aplicar únicamente las inserciones, eliminaciones y cambios detectados, sin invalidar la lista completa.
4. WHEN el Repositorio_Noticias obtiene datos remotos o accede a la Cache_Noticias, THE Repositorio_Noticias SHALL ejecutar esas operaciones en el @IoExecutor y SHALL NOT ejecutarlas en el hilo principal, de modo que no se produzca NetworkOnMainThreadException.
5. IF el Repositorio_Noticias encuentra un fallo de red, del proveedor o de persistencia, THEN THE Repositorio_Noticias SHALL propagar el fallo como un Result tipado de error (domain/common/Result) que identifique la categoría del fallo, sin lanzar excepciones no controladas al llamador, de modo que los criterios 9.4 y 10.3 sean satisfacibles.
6. WHEN el Repositorio_Noticias devuelve un Result tipado, THE ViewModel_Noticias SHALL transformar el Result de éxito o de error en el Estado_UI correspondiente y SHALL exponerlo de forma observable mediante LiveData.

### Requisito 12: Estilo visual coherente con la app

**Historia de Usuario:** Como usuario, quiero que la Pantalla_Noticias tenga la misma estética oscura que el resto de la app, para una experiencia visual consistente.

#### Criterios de Aceptación

1. THE Pantalla_Noticias SHALL aplicar el tema oscuro de la app usando los colores definidos en la paleta compartida (blue_dark, blue_start, blue_end) para fondo y elementos principales, sin declarar valores de color propios fuera de dicha paleta.
2. THE Pantalla_Noticias SHALL mostrar en la cabecera el título "Noticias Deportivas" junto a un control de búsqueda accesible con descripción de contenido no vacía.
3. WHEN el usuario activa el control de búsqueda, THE Pantalla_Noticias SHALL mostrar el campo de entrada de búsqueda y SHALL trasladar el foco de entrada a dicho campo.
4. THE Pantalla_Noticias SHALL mostrar bajo el título una fila horizontal desplazable de Chip_Liga que contenga un chip por cada liga disponible, indicando de forma visible el chip seleccionado.
5. THE Adaptador_Noticias SHALL renderizar cada tarjeta de noticia con una imagen de fondo, un badge de categoría/liga y el texto del titular, usando los colores de la paleta oscura de la app (blue_dark, blue_start, blue_end).
6. WHILE la imagen de fondo de una tarjeta se está cargando o su carga falla, THE Adaptador_Noticias SHALL mostrar una imagen de marcador de posición coherente con la paleta oscura, manteniendo visibles el badge y el titular.

### Requisito 13: Requisitos no funcionales

**Historia de Usuario:** Como desarrollador, quiero que la Pantalla_Noticias cumpla requisitos no funcionales de rendimiento, accesibilidad, seguridad de red e idioma, para ofrecer una experiencia fluida, accesible y segura de forma sostenible.

#### Criterios de Aceptación

1. THE Adaptador_Noticias SHALL cargar las imágenes de las tarjetas mediante una librería de carga de imágenes con caché en disco y memoria (a seleccionar en el diseño), de modo que el desplazamiento no vuelva a descargar imágenes ya vistas.
2. THE Pantalla_Noticias SHALL proporcionar `contentDescription` no vacía para el control de búsqueda, cada Chip_Liga y cada tarjeta de noticia, de modo que sean navegables con TalkBack.
3. THE App SHALL mantener deshabilitado el tráfico HTTP en claro (`usesCleartextTraffic=false`); WHEN un Articulo_Noticia tenga URL de imagen o de artículo con esquema `http://`, THE App SHALL promover a `https` cuando sea posible o tratar el recurso como no cargable.
4. THE Servicio_API_Noticias SHALL solicitar noticias con `lang=es`; se acepta como compromiso conocido menor volumen para ligas con más cobertura en inglés (Premier League, Bundesliga), documentado como decisión.
