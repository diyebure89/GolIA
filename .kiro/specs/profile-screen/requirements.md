# Requirements Document

## Introduction

Esta funcionalidad construye desde cero la pantalla "Perfil" (PerfilFragment) de la app GolIA (predicciones de fútbol, Android, Java, Clean Architecture + MVVM + Hilt + Room). Hoy `fragment_perfil.xml` es un marcador de posición vacío; el objetivo es materializar la pantalla del mockup: un avatar circular con una insignia de lápiz para editar la foto, un formulario para editar los datos personales, un flujo separado para cambiar la contraseña, una sección de "Historial" (ranking) alineada con la fuente de datos compartida de la bienvenida de Inicio, y un botón de "Cerrar sesión".

El alcance es local (Room/SQLite), sin backend remoto. La pantalla identifica al usuario actual a partir de la sesión local (`PreferencesManager`, que persiste `user_id`, `user_name`, `is_logged_in`, `access_token`, `refresh_token` y `token_expiry`). La comprobación de sesión válida usa el contrato real `PreferencesManager.isLoggedIn()`, que devuelve verdadero solo si `is_logged_in == true` Y existe un `access_token` no vacío. El nombre completo se muestra en modo de solo lectura y no se edita desde esta pantalla; los datos personales editables (correo y nombre de usuario, más la contraseña en un flujo aparte) se persisten en la tabla `users`. Como el `UserDao` actual NO tiene métodos de actualización (solo insert, findByEmail, findByUsername, findByUsernameOrEmail, existsByEmail, existsByUsername y getById), esta funcionalidad añade consultas de actualización por `id`, re-hashea la contraseña al cambiarla mediante el `PasswordHasher` existente (PBKDF2), y vuelve a comprobar la unicidad de correo y nombre de usuario excluyendo al propio usuario.

La foto de perfil se copia al almacenamiento interno de la app (`filesDir`) y su RUTA ABSOLUTA se persiste en una nueva columna `avatar_uri` de la tabla `users` (el modelo de dominio `User` ya expone `avatarUrl`, pero hoy no se persiste). La sección de "Historial" se maqueta con las mismas métricas placeholder que la bienvenida de Inicio (pronósticos, aciertos, puntos, ranking) y consume la misma interfaz de dominio de ranking provista por Hilt como singleton, para conectar el ranking real más adelante. "Cerrar sesión" limpia la sesión local de forma atómica y redirige a la pantalla de inicio de sesión (LoginActivity). Las validaciones reutilizan el componente de validación del dominio existente (`RegistrationValidator`) y los errores viajan como un contrato tipado que la UI traduce a mensajes localizados.

Decisiones de producto confirmadas para esta entrega:
- El cambio de contraseña EXIGE la contraseña actual (verificada con `PasswordHasher`) antes de fijar la nueva.
- La foto puede provenir de la galería o de la cámara (la cámara usa `FileProvider`).
- La foto se copia a almacenamiento interno y se persiste su ruta absoluta en una nueva columna `avatar_uri`; nunca se persiste el `content://` URI de origen.
- El nombre completo se muestra en modo de solo lectura y no es editable desde esta pantalla; los datos personales editables (correo y nombre de usuario) se guardan juntos con un único botón "Guardar cambios"; la contraseña se gestiona en un flujo aparte.
- La sección "Historial" muestra placeholders consistentes con la bienvenida de Inicio, sobre una fuente de datos compartida.
- El `Full_Name` mostrado proviene de la columna `full_name` de `Local_Database` (verdad única), no del `user_name` cifrado de la sesión.

## Glossary

- **Perfil_UI**: La pantalla `PerfilFragment` y su `PerfilViewModel`, responsables de mostrar los datos del usuario actual, recibir las ediciones del formulario, reflejar estados de carga/éxito/error y disparar la navegación de cierre de sesión. Es una vista delgada que no accede a repositorios ni a la base de datos directamente.
- **Profile_System**: Componente del dominio y datos que carga, valida y persiste las actualizaciones del perfil del usuario actual en la base de datos local (casos de uso de perfil + `AuthRepository`/repositorio de usuario + `UserDao`/Room).
- **Local_Session**: La sesión local del usuario gestionada por `PreferencesManager`, que persiste `user_id`, `user_name`, `is_logged_in`, `access_token`, `refresh_token` y `token_expiry`. Identifica al usuario actual. La validez de sesión se determina con `PreferencesManager.isLoggedIn()` (verdadero solo si `is_logged_in == true` Y `access_token` no está vacío). Al cerrar sesión se limpia de forma atómica mediante `PreferencesManager.clearTokens()`, que borra en una sola operación `access_token`, `refresh_token`, `user_id`, `user_name`, `is_logged_in` y `token_expiry`.
- **Local_Database**: La base de datos local del dispositivo gestionada por Room (SQLite), `GolIADatabase`, que contiene la tabla `users` (`UserEntity`/`UserDao`). El acceso se realiza a través de la interfaz de repositorio del dominio. El proyecto no usa MySQL.
- **Current_User**: El usuario autenticado cuya identidad (`id`) se obtiene de `Local_Session` (`PreferencesManager.getUserId()`) y cuya sesión se valida con `PreferencesManager.isLoggedIn()`; sus datos se leen de `Local_Database` por clave primaria.
- **Password_Hasher**: Componente del dominio (`PasswordHasher`) que deriva y verifica credenciales de contraseña con PBKDF2WithHmacSHA256, salt aleatorio y comparación en tiempo constante, sin exponer la contraseña en texto plano.
- **Password_Credential**: Valor inmutable (`PasswordCredential`) que agrupa algoritmo, iteraciones, salt (Base64) y hash (Base64) de una contraseña, almacenado en las columnas de credenciales de `UserEntity`.
- **Profile_Validator**: Componente de validación reutilizable del dominio para los campos editables del perfil (nombre de usuario, correo, contraseña nueva y confirmación). Reutiliza las mismas reglas que el registro (`Registration_Validator`), incluidas las reglas comunes que se añadan al validador compartido.
- **Registration_Validator**: Componente de validación compartido del dominio (`RegistrationValidator`) usado por registro y perfil. Para el nombre de usuario evalúa el FORMATO (conjunto `^[A-Za-z0-9_.]+$`) ANTES que la longitud (mínimo 3, máximo 30). Para la contraseña exige mínimo 8 caracteres (`PASSWORD_MIN=8`), al menos una mayúscula, una minúscula y un dígito; esta funcionalidad añade a este validador compartido una regla común de longitud máxima de 64 caracteres para la contraseña, que pasa a aplicarse tanto en registro como en perfil.
- **Auth_Error**: Conjunto tipado de errores que la capa de UI traduce a mensajes localizados. Códigos reales existentes: `USERNAME_TAKEN`, `EMAIL_TAKEN`, `INVALID_CREDENTIALS`, `PERSISTENCE_ERROR`, `VALIDATION_ERROR`. Esta funcionalidad amplía el contrato tipado con `SESSION_UNAVAILABLE` (no hay sesión válida al operar) y `SESSION_CLEAR_FAILED` (fallo al cerrar sesión).
- **Full_Name**: Nombre completo del usuario, almacenado en la columna `full_name` de `Local_Database` (fuente de verdad para su visualización). Se muestra en modo de solo lectura en la pantalla de perfil y no es editable desde esta pantalla. No se toma del `user_name` de la sesión.
- **Username**: Nombre de usuario OPCIONAL. Reglas: si se proporciona, 3 a 30 caracteres del conjunto `[A-Za-z0-9_.]` con un `@` inicial opcional que se retira; se normaliza a minúsculas; único entre valores no nulos. El FORMATO se evalúa antes que la longitud. Vacío se persiste como NULL.
- **Email**: Correo electrónico único y obligatorio. Se normaliza a minúsculas (trim + toLowerCase) antes de validar unicidad y de persistir; debe cumplir `android.util.Patterns.EMAIL_ADDRESS`.
- **Current_Password**: Contraseña actual en texto plano que el usuario introduce para autorizar un cambio de contraseña; se verifica con `Password_Hasher` contra la credencial almacenada.
- **New_Password**: Contraseña nueva en texto plano. Reglas (compartidas con `Registration_Validator`): de 8 a 64 caracteres, con al menos una mayúscula, una minúscula y un número.
- **Confirm_New_Password**: Segunda entrada que debe coincidir exactamente con `New_Password`.
- **Profile_Photo**: La imagen de avatar del usuario. Se selecciona de galería o cámara (la cámara usa `FileProvider`), se corrige su orientación EXIF, se reescala a una resolución objetivo, se recomprime y se copia al almacenamiento interno de la app; su ruta absoluta se persiste en la columna `avatar_uri`.
- **Avatar_Uri**: Nueva columna (`avatar_uri`) de la tabla `users` que almacena la RUTA ABSOLUTA del archivo de foto de perfil copiado al almacenamiento interno (`filesDir`); nula cuando no hay foto. Nunca almacena el `content://` URI de origen.
- **File_Provider**: `androidx.core.content.FileProvider` usado para exponer de forma segura el archivo destino de la captura de cámara mediante un URI de contenido autorizado, evitando exponer rutas de archivo directas a otras apps.
- **Historial_Section**: La sección "Historial" de `Perfil_UI` que presenta las métricas de ranking del usuario (pronósticos, aciertos, puntos, ranking).
- **Ranking_Data_Source**: Interfaz de dominio (`RankingDataSource`) de la que provienen las métricas de ranking mostradas tanto en la bienvenida de Inicio como en `Historial_Section`. Se provee mediante Hilt como singleton, de modo que Inicio y Perfil consumen el mismo binding sin acoplarse a detalles de instanciación. En esta entrega su implementación expone valores placeholder consistentes y queda preparada para conectar el ranking real.
- **Login_Screen**: La pantalla `LoginActivity` a la que se redirige tras cerrar sesión.
- **User_Name_Session**: El valor `user_name` almacenado en `Local_Session`. Está cifrado con AES-GCM mediante el Android Keystore (`saveUserName()`/`getUserName()`) y se usa exclusivamente para la bienvenida de la pantalla de Inicio; no es la fuente del `Full_Name` del perfil.
- **MVVM (Model-View-ViewModel)**: Patrón de presentación que separa la vista (Fragment) de la lógica de presentación (ViewModel) y de los datos (Model). El ViewModel expone el estado como observables (LiveData) y sobrevive a cambios de configuración.
- **Hilt**: Framework de inyección de dependencias para Android que provee repositorios, DAOs, casos de uso, ViewModels y bindings singleton (como `Ranking_Data_Source`) mediante anotaciones.

## Requirements

### Requirement 1: Identificación y carga del usuario actual

**User Story:** Como usuario que ha iniciado sesión, quiero que la pantalla de perfil muestre mis datos actuales, para poder revisarlos y editarlos.

#### Acceptance Criteria

1. WHEN `Perfil_UI` se hace visible, THE `Profile_System` SHALL comprobar la validez de la sesión mediante `PreferencesManager.isLoggedIn()` y, cuando sea válida, obtener el identificador del `Current_User` mediante `PreferencesManager.getUserId()`.
2. WHEN el `Profile_System` dispone del identificador del `Current_User`, THE `Profile_System` SHALL leer el usuario correspondiente de `Local_Database` por clave primaria a través de la interfaz de repositorio del dominio, ejecutando la lectura fuera del hilo principal, y THE `Perfil_UI` SHALL mostrar un indicador de carga mientras la lectura está en curso.
3. WHEN el `Profile_System` obtiene los datos del `Current_User`, THE `Perfil_UI` SHALL mostrar el `Full_Name` leído de la columna `full_name` de `Local_Database` en un control de solo lectura (no editable) y el `Email` y el `Username` (o una indicación de "sin nombre de usuario" cuando sea NULL) en campos editables del formulario.
4. THE `Perfil_UI` SHALL tomar el `Full_Name` mostrado exclusivamente de la columna `full_name` de `Local_Database` y SHALL NOT tomarlo del `User_Name_Session` (`PreferencesManager.getUserName()`) de la sesión.
5. WHEN el `Profile_System` obtiene los datos del `Current_User` y el `Full_Name` o el `Email` almacenados son NULL, THE `Perfil_UI` SHALL mostrar el campo correspondiente vacío y SHALL NOT mostrar el literal "null".
6. WHEN el `Current_User` tiene un `Avatar_Uri` no nulo cuyo archivo existe, THE `Perfil_UI` SHALL mostrar la `Profile_Photo` en el avatar circular.
7. IF el `Current_User` no tiene `Avatar_Uri` o el archivo referenciado no existe, THEN THE `Perfil_UI` SHALL mostrar una imagen de avatar por defecto.
8. IF `PreferencesManager.isLoggedIn()` devuelve falso, o `getUserId()` no contiene un identificador válido, o no existe en `Local_Database` un usuario con ese identificador, THEN THE `Perfil_UI` SHALL redirigir a `Login_Screen` y SHALL NOT mostrar datos de perfil.
9. IF `getUserId()` devuelve un identificador no nulo pero `PreferencesManager.isLoggedIn()` devuelve falso, THEN THE `Perfil_UI` SHALL redirigir a `Login_Screen` y SHALL NOT mostrar datos de perfil.
10. IF ocurre un error técnico al leer los datos del `Current_User` de `Local_Database`, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error indicando que no se pudieron cargar los datos y SHALL NOT mostrar datos de perfil parciales.
11. WHILE el `Profile_System` carga los datos del `Current_User`, THE `Perfil_UI` SHALL mostrar un indicador de carga.

### Requirement 2: Visualización del nombre completo (solo lectura)

**User Story:** Como usuario, quiero ver mi nombre completo en el perfil, aunque no pueda editarlo desde esta pantalla.

#### Acceptance Criteria

1. THE `Perfil_UI` SHALL mostrar el `Full_Name` del `Current_User`, leído de la columna `full_name` de `Local_Database`, en un control de solo lectura (no editable).
2. THE `Perfil_UI` SHALL NOT permitir al usuario modificar el `Full_Name` desde la pantalla de perfil.
3. THE `Perfil_UI` SHALL NOT usar el `User_Name_Session` cifrado (`PreferencesManager.getUserName()`) como fuente del `Full_Name` mostrado.
4. WHEN el usuario pulsa "Guardar cambios", THE `Profile_System` SHALL NOT incluir el `Full_Name` entre los campos que se actualizan ni SHALL modificar la columna `full_name` del `Current_User`.

### Requirement 3: Edición del nombre de usuario

**User Story:** Como usuario, quiero editar (o dejar vacío) mi nombre de usuario, para elegir un identificador público predecible o no tener ninguno.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de perfil, THE `Profile_Validator` SHALL normalizar el `Username` eliminando los espacios iniciales y finales (trim) y retirando un único carácter `@` inicial si estuviera presente, antes de evaluar cualquier regla de validación.
2. IF el `Username` normalizado tiene longitud cero (vacío o compuesto únicamente por espacios en blanco), THEN THE `Perfil_UI` SHALL aceptar el valor sin mostrar error en el campo de nombre de usuario, tratando el `Username` como no proporcionado y SHALL NOT evaluar las reglas de formato ni de longitud.
3. IF el `Username` normalizado no está vacío y contiene algún carácter fuera del conjunto `[A-Za-z0-9_.]`, THEN THE `Perfil_UI` SHALL mostrar un error de formato en el campo de nombre de usuario, SHALL rechazar el guardado y SHALL NOT evaluar las reglas de longitud.
4. IF el `Username` normalizado no está vacío, todos sus caracteres pertenecen al conjunto `[A-Za-z0-9_.]` y contiene menos de 3 caracteres, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error en el campo de nombre de usuario indicando que debe tener al menos 3 caracteres y SHALL rechazar el guardado.
5. IF el `Username` normalizado no está vacío, todos sus caracteres pertenecen al conjunto `[A-Za-z0-9_.]` y contiene más de 30 caracteres, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error en el campo de nombre de usuario indicando que no debe superar los 30 caracteres y SHALL rechazar el guardado.
6. WHEN el `Username` normalizado tiene todos sus caracteres dentro del conjunto `[A-Za-z0-9_.]` y una longitud de 3 a 30 caracteres, THE `Profile_Validator` SHALL aceptar el valor de `Username` como válido.

### Requirement 4: Edición del correo electrónico

**User Story:** Como usuario, quiero editar mi correo electrónico, para que mi cuenta refleje una dirección válida y actual.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de perfil, THE `Profile_Validator` SHALL normalizar el `Email` eliminando los espacios iniciales y finales (trim) antes de evaluar cualquier regla de validación.
2. IF el `Email` normalizado tiene longitud cero (vacío o compuesto únicamente por espacios en blanco), THEN THE `Perfil_UI` SHALL mostrar un mensaje de error en el campo de correo indicando que el correo electrónico es obligatorio, SHALL rechazar el guardado y SHALL NOT evaluar la regla de formato.
3. IF el `Email` normalizado no está vacío y no cumple el patrón `android.util.Patterns.EMAIL_ADDRESS`, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error en el campo de correo indicando que el formato del correo electrónico es inválido, y SHALL rechazar el guardado.
4. WHEN el usuario envía el formulario con un `Email` normalizado que cumple el patrón `android.util.Patterns.EMAIL_ADDRESS`, THE `Profile_Validator` SHALL aceptar el valor de `Email` como válido.

### Requirement 5: Unicidad y normalización al actualizar correo y nombre de usuario

**User Story:** Como sistema, quiero garantizar que el correo y el nombre de usuario sigan siendo únicos y normalizados tras una edición, para evitar cuentas duplicadas o ambiguas.

#### Acceptance Criteria

1. WHEN el `Profile_System` procesa una actualización, THE `Profile_System` SHALL normalizar `Email` y `Username` aplicando, en este orden, recorte de espacios inicial y final (trim) y conversión a minúsculas con reglas de localidad invariante (independiente de la configuración regional del dispositivo), y SHALL retirar un único carácter `@` inicial del `Username` si está presente, antes de validar unicidad y antes de persistir.
2. WHEN el `Profile_System` valida la unicidad del `Email` o del `Username`, THE `Profile_System` SHALL excluir de la comprobación al propio `Current_User` (comparando por `id`), de modo que conservar el mismo valor no se considere un conflicto.
3. IF el `Email` normalizado ya pertenece a un usuario con `id` distinto del `Current_User` en `Local_Database`, THEN THE `Profile_System` SHALL rechazar la actualización, SHALL devolver al llamador un resultado de error tipado `EMAIL_TAKEN` que identifique el campo en conflicto, y SHALL NOT modificar ninguna columna del `Current_User`.
4. IF el `Username` normalizado ya pertenece a un usuario con `id` distinto del `Current_User` entre los valores no nulos de `Local_Database`, THEN THE `Profile_System` SHALL rechazar la actualización, SHALL devolver al llamador un resultado de error tipado `USERNAME_TAKEN` que identifique el campo en conflicto, y SHALL NOT modificar ninguna columna del `Current_User`.
5. WHEN el `Username` normalizado resulta una cadena de longitud cero, THE `Profile_System` SHALL persistir NULL en la columna de nombre de usuario y SHALL NOT persistir una cadena vacía.
6. IF la escritura en `Local_Database` viola una restricción de unicidad de `Email` o de `Username` (`SQLiteConstraintException`), THEN THE `Profile_System` SHALL mapear la excepción a un resultado de error tipado (`EMAIL_TAKEN` si el conflicto es de `Email`, `USERNAME_TAKEN` si es de `Username`) y SHALL dejar el registro del `Current_User` en su estado previo a la actualización, sin datos parciales.

### Requirement 6: Persistencia de los datos personales

**User Story:** Como usuario, quiero que al pulsar "Guardar cambios" mis datos personales se persistan, para que mis ediciones sobrevivan al cierre de la app.

#### Acceptance Criteria

1. THE `UserDao` SHALL definir una consulta de actualización de datos personales por `id`, del tipo `@Query("UPDATE users SET username = :username, email = :email WHERE id = :id")`, que actualice el registro del `Current_User` sin tocar otras columnas.
2. WHEN el usuario pulsa "Guardar cambios" y todas las validaciones de `Username` y `Email` se superan y no existen conflictos de unicidad, THE `Profile_System` SHALL actualizar en `Local_Database` el registro del `Current_User` con el `Username` normalizado (o NULL) y el `Email` normalizado, mediante la consulta de actualización de datos personales por `id` del `UserDao`.
3. WHEN el `Profile_System` actualiza los datos personales, THE `Local_Database` SHALL conservar sin cambios el identificador (`id`), la marca de creación (`created_at`), la columna `full_name`, la columna `avatar_uri` y las columnas de credencial de contraseña del `Current_User`.
4. WHEN la actualización se persiste correctamente, THE `Perfil_UI` SHALL mostrar una confirmación de guardado exitoso mediante un `Toast` con una duración corta (equivalente a `Toast.LENGTH_SHORT`, aproximadamente 2 segundos).
5. IF la escritura en `Local_Database` lanza `SQLiteConstraintException` por unicidad durante la actualización, THEN THE `Profile_System` SHALL mapearla a `EMAIL_TAKEN` o `USERNAME_TAKEN` según el campo en conflicto y SHALL dejar el registro del `Current_User` en su estado previo, sin datos parciales.
6. IF ocurre un error técnico distinto de una violación de unicidad al escribir en `Local_Database` durante la actualización, THEN THE `Profile_System` SHALL devolver al llamador un resultado de error tipado `PERSISTENCE_ERROR` que indique el campo o la operación fallida, y SHALL dejar el registro del `Current_User` en su estado previo a la actualización, sin datos parciales.
7. IF el `Profile_System` recibe un resultado de error `PERSISTENCE_ERROR`, `EMAIL_TAKEN` o `USERNAME_TAKEN` durante el guardado, THEN THE `Perfil_UI` SHALL mostrar al usuario un mensaje de error que indique el motivo del fallo y SHALL mantener los datos editados visibles en el formulario para permitir un nuevo intento.
8. THE `Profile_System` SHALL acceder a la persistencia únicamente a través de la interfaz de repositorio del dominio, de modo que la implementación de almacenamiento quede aislada del dominio y de la presentación.

### Requirement 7: Cambio de contraseña con contraseña actual

**User Story:** Como usuario, quiero cambiar mi contraseña introduciendo primero la actual, para que solo yo pueda modificarla.

#### Acceptance Criteria

1. WHEN el usuario inicia el flujo de cambio de contraseña, THE `Perfil_UI` SHALL presentar campos separados para `Current_Password`, `New_Password` y `Confirm_New_Password`.
2. IF el usuario envía el cambio de contraseña con `Current_Password` vacío o compuesto únicamente por espacios en blanco, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error indicando que la contraseña actual es obligatoria, SHALL rechazar el cambio y SHALL conservar sin modificar el contenido de los demás campos.
3. IF el usuario envía el cambio de contraseña con `New_Password` vacío o compuesto únicamente por espacios en blanco, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error indicando que la nueva contraseña es obligatoria, SHALL rechazar el cambio y SHALL conservar sin modificar el contenido de los demás campos.
4. IF la `New_Password` incumple las reglas de complejidad del `Registration_Validator` (menos de 8 caracteres, más de 64 caracteres, o carencia de al menos una mayúscula, una minúscula y un número), THEN THE `Perfil_UI` SHALL mostrar el mensaje de error de complejidad correspondiente y SHALL rechazar el cambio. La regla de longitud máxima de 64 caracteres se añade al `Registration_Validator` compartido, por lo que aplica de forma idéntica a registro y perfil.
5. IF `Confirm_New_Password` está vacío o no coincide exactamente, carácter por carácter, con `New_Password`, THEN THE `Perfil_UI` SHALL mostrar un mensaje de error indicando que las contraseñas no coinciden, y SHALL rechazar el cambio.
6. IF al enviar el cambio de contraseña `PreferencesManager.isLoggedIn()` devuelve falso o no existe un `Current_User` con sesión válida, THEN THE `Profile_System` SHALL rechazar el cambio, SHALL NOT modificar la contraseña almacenada, SHALL devolver un error tipado `SESSION_UNAVAILABLE` y THE `Perfil_UI` SHALL mostrar un mensaje localizado indicando que la sesión no está disponible.
7. WHEN el usuario envía el cambio de contraseña y todas las validaciones de formato de los criterios 2 a 5 se han superado, THE `Password_Hasher` SHALL verificar `Current_Password` contra el `Password_Credential` almacenado del `Current_User` usando comparación en tiempo constante, ejecutando la verificación fuera del hilo principal, y THE `Perfil_UI` SHALL mostrar un indicador de carga mientras la verificación está en curso.
8. WHILE el `Profile_System` está procesando un cambio de contraseña ya enviado, THE `Perfil_UI` SHALL deshabilitar el control de envío para impedir un segundo envío hasta que el procesamiento finalice con éxito o con error.
9. IF la verificación de `Current_Password` falla, THEN THE `Profile_System` SHALL devolver un error `INVALID_CREDENTIALS`, SHALL NOT modificar la contraseña almacenada y THE `Perfil_UI` SHALL mostrar un mensaje de error indicando que la contraseña actual es incorrecta.
10. IF la verificación de `Current_Password` falla 5 veces consecutivas contadas por un contador de intentos fallidos IN-MEMORY con alcance del `PerfilViewModel` (por sesión de pantalla), THEN THE `Profile_System` SHALL bloquear nuevos intentos de cambio durante 60 segundos y THE `Perfil_UI` SHALL mostrar un mensaje de error indicando que debe esperar antes de reintentar. Este contador es una mitigación de experiencia de usuario y no una defensa de seguridad; WHEN la vista o el `PerfilViewModel` se recrean, THE `Profile_System` SHALL reiniciar el contador a cero.
11. WHEN la verificación de `Current_Password` es correcta, THE `Password_Hasher` SHALL derivar un nuevo `Password_Credential` para la `New_Password` con un salt aleatorio nuevo, y THE `Profile_System` SHALL actualizar en `Local_Database` únicamente las columnas de credencial (algoritmo, iteraciones, salt y hash) del `Current_User` mediante la consulta de actualización de credenciales por `id` del `UserDao`.
12. THE `Profile_System` SHALL NOT almacenar la `New_Password` ni la `Current_Password` en texto plano en ningún campo de `Local_Database`.
13. WHEN la contraseña se actualiza correctamente, THE `Perfil_UI` SHALL mostrar una confirmación mediante un `Toast` y SHALL limpiar los campos `Current_Password`, `New_Password` y `Confirm_New_Password`.

### Requirement 8: Selección de la foto de perfil

**User Story:** Como usuario, quiero elegir mi foto de perfil desde la galería o la cámara, para personalizar mi cuenta.

#### Acceptance Criteria

1. WHEN el usuario pulsa la insignia de lápiz sobre el avatar, THE `Perfil_UI` SHALL mostrar en un plazo máximo de 1 segundo un selector con exactamente dos opciones: seleccionar una imagen de la galería y tomar una foto con la cámara.
2. WHEN el usuario elige tomar una foto y la app no dispone aún del permiso de cámara, THE `Perfil_UI` SHALL solicitar el permiso de cámara antes de abrir la cámara.
3. WHEN el usuario elige tomar una foto y el permiso de cámara está concedido, THE `Perfil_UI` SHALL abrir la cámara usando un `File_Provider` para exponer el archivo destino mediante un URI de contenido autorizado.
4. IF el usuario deniega el permiso de cámara, THEN THE `Perfil_UI` SHALL mostrar un aviso indicando que no se puede usar la cámara sin el permiso y SHALL NOT abrir la cámara.
5. IF el usuario deniega el permiso de cámara de forma permanente, THEN THE `Perfil_UI` SHALL mostrar un aviso indicando que el permiso debe habilitarse desde los ajustes del sistema y SHALL NOT abrir la cámara.
6. WHEN el usuario selecciona una imagen de la galería o captura una foto con la cámara y la imagen es de un formato soportado, THE `Profile_System` SHALL decodificarla acotando las dimensiones de decodificación para evitar errores de memoria (OOM), corregir la orientación EXIF, reescalarla (downscale) a una resolución objetivo de 512×512 conservando proporción, recomprimirla y copiar el resultado a un archivo dentro del almacenamiento interno de la app.
7. THE `Profile_System` SHALL aceptar únicamente los formatos de imagen JPEG, PNG y WEBP.
8. IF la imagen seleccionada no es de un formato soportado (JPEG, PNG, WEBP) o supera un tamaño máximo de 10 MB, THEN THE `Profile_System` SHALL rechazar la imagen, SHALL mostrar un aviso indicando el motivo del rechazo y SHALL conservar la `Profile_Photo` actual sin cambios.
9. IF el usuario cancela la selección de galería o la captura de cámara sin elegir una imagen, THEN THE `Perfil_UI` SHALL conservar la `Profile_Photo` actual sin cambios.

### Requirement 9: Persistencia y visualización de la foto de perfil

**User Story:** Como usuario, quiero que mi foto de perfil se guarde y se muestre al volver a abrir la app, para que mi personalización sea permanente.

#### Acceptance Criteria

1. THE `Local_Database` SHALL definir en la tabla `users` una columna `avatar_uri` nullable que almacene la RUTA ABSOLUTA del archivo de `Profile_Photo` copiado al almacenamiento interno (`filesDir`).
2. THE `Profile_System` SHALL persistir en `avatar_uri` la ruta absoluta del archivo copiado a `filesDir` y SHALL NOT persistir el `content://` URI de origen de la galería ni de la cámara.
3. WHEN el `Profile_System` copia una nueva `Profile_Photo` a almacenamiento interno, THE `Profile_System` SHALL escribir el archivo con una convención de nombres determinista basada en el `id` del `Current_User` (por ejemplo `avatar_{userId}.jpg`) y SHALL actualizar la columna `avatar_uri` del `Current_User` con la ruta absoluta del nuevo archivo mediante la consulta de actualización de `avatar_uri` por `id` del `UserDao`.
4. THE `UserDao` SHALL definir una consulta de actualización de `avatar_uri` por `id`, del tipo `@Query("UPDATE users SET avatar_uri = :avatarUri WHERE id = :id")`.
5. WHEN la columna `avatar_uri` se actualiza correctamente, THE `Perfil_UI` SHALL mostrar la nueva `Profile_Photo` en el avatar circular en un plazo máximo de 1 segundo y sin requerir reiniciar la app.
6. WHEN `Perfil_UI` se vuelve a abrir y el `Current_User` tiene un `avatar_uri` cuyo archivo existe, THE `Perfil_UI` SHALL cargar y mostrar esa imagen en el avatar circular.
7. IF el `Current_User` tiene un `avatar_uri` no nulo cuyo archivo no existe en almacenamiento interno, THEN THE `Perfil_UI` SHALL mostrar el avatar por defecto y SHALL NOT mostrar un estado de error al usuario.
8. IF ocurre un error al copiar el archivo o al actualizar `avatar_uri`, THEN THE `Profile_System` SHALL devolver un resultado de error `PERSISTENCE_ERROR`, THE `Perfil_UI` SHALL conservar la `Profile_Photo` previa y THE `Perfil_UI` SHALL mostrar un aviso indicando que no se pudo guardar la foto.
9. WHEN se persiste una nueva `Profile_Photo` que reemplaza a una anterior copiada a almacenamiento interno, THE `Profile_System` SHALL eliminar el archivo de imagen anterior, de forma coherente con la convención de nombres determinista, para no acumular archivos huérfanos.

### Requirement 10: Migración de esquema de la base de datos para la foto de perfil

**User Story:** Como usuario existente, quiero que al actualizar la app mis datos de cuenta se conserven al añadirse la columna de foto, para no perder mi cuenta.

#### Acceptance Criteria

1. THE `Local_Database` (`GolIADatabase`) SHALL incrementar la versión del esquema de Room de 4 a 5 para incorporar la columna `avatar_uri` en la tabla `users`.
2. THE `Local_Database` SHALL registrar en el `databaseBuilder` de Room una migración `MIGRATION_4_5` junto a las migraciones existentes (`MIGRATION_2_3`, `MIGRATION_3_4`).
3. WHEN la app se actualiza desde la versión 4 (sin la columna `avatar_uri`), THE `MIGRATION_4_5` SHALL añadir la columna `avatar_uri` conservando el 100% de los registros de usuario existentes sin alterar sus valores previos.
4. WHEN un usuario existente se carga tras la migración, THE `Local_Database` SHALL presentar `avatar_uri` como NULL para las cuentas creadas antes de la migración.
5. THE `Local_Database` SHALL mantener `exportSchema = true` y SHALL exportar/regenerar el archivo JSON de esquema de la versión 5 en el directorio de esquemas del módulo.
6. THE proyecto SHALL incluir un test de migración v4→v5 análogo a `MatchMigrationTest` que verifique que la migración se aplica correctamente y que los datos de usuario existentes se conservan.
7. IF la migración del esquema falla, THEN THE `Local_Database` SHALL preservar los datos de usuario existentes sin modificarlos y SHALL devolver un resultado de error indicando el fallo de migración.

### Requirement 11: Sección de Historial con datos de ranking compartidos

**User Story:** Como usuario, quiero ver mi historial/ranking en el perfil, para conocer mi rendimiento; y quiero que coincidan con lo que aparece en la bienvenida de Inicio.

#### Acceptance Criteria

1. WHEN el usuario abre `Perfil_UI`, THE `Perfil_UI` SHALL presentar una `Historial_Section` que muestre las cuatro métricas de ranking del usuario: pronósticos realizados, porcentaje de aciertos, puntos totales y posición de ranking.
2. THE `Historial_Section` SHALL mostrar el número de pronósticos realizados como un entero mayor o igual a 0, el porcentaje de aciertos como un valor entre 0 y 100 con un máximo de 1 decimal, los puntos totales como un valor mayor o igual a 0, y la posición de ranking como un entero mayor o igual a 1.
3. THE `Historial_Section` SHALL obtener las métricas de ranking a través de la misma interfaz de dominio `Ranking_Data_Source` provista por Hilt como singleton que alimenta la bienvenida de Inicio, de modo que Inicio y Perfil consuman el mismo binding.
4. WHILE el ranking real no esté implementado, THE `Ranking_Data_Source` SHALL exponer valores placeholder idénticos, campo por campo, a los mostrados en la bienvenida de Inicio.
5. IF `Ranking_Data_Source` no dispone de métricas de ranking para el `Current_User`, THEN THE `Historial_Section` SHALL mostrar valores placeholder o un estado neutro y SHALL NOT mostrar un mensaje de error al usuario.
6. THE `Ranking_Data_Source` SHALL exponer las métricas mediante una interfaz del dominio, de modo que conectar la fuente real de ranking más adelante no requiera cambios en `Perfil_UI`.

### Requirement 12: Cierre de sesión y redirección al login

**User Story:** Como usuario, quiero cerrar sesión desde el perfil, para salir de mi cuenta y volver a la pantalla de inicio de sesión.

#### Acceptance Criteria

1. THE `Perfil_UI` SHALL presentar un botón de "Cerrar sesión".
2. WHEN el usuario pulsa "Cerrar sesión", THE `Profile_System` SHALL limpiar la `Local_Session` invocando `PreferencesManager.clearTokens()` como una única operación atómica.
3. WHEN el `Profile_System` invoca `PreferencesManager.clearTokens()`, THE `Local_Session` SHALL borrar en esa misma operación atómica `access_token`, `refresh_token`, `user_id`, `user_name`, `is_logged_in` y `token_expiry`, sin dejar un subconjunto de esos valores persistidos.
4. WHEN la `Local_Session` se ha limpiado correctamente, THE `Perfil_UI` SHALL navegar a `Login_Screen` (`LoginActivity`) estableciendo indicadores de tarea que eliminen las pantallas anteriores de la pila (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`), de modo que el botón atrás no regrese a la sesión cerrada.
5. IF la limpieza de la `Local_Session` falla, THEN THE `Profile_System` SHALL preservar el estado de sesión existente, SHALL devolver un error tipado `SESSION_CLEAR_FAILED`, THE `Perfil_UI` SHALL permanecer en la pantalla de perfil y SHALL mostrar un mensaje localizado indicando que no se pudo cerrar la sesión.
6. THE `Profile_System` SHALL NOT eliminar el registro del `Current_User` de `Local_Database` al cerrar sesión.

### Requirement 13: Estados de la interfaz durante las operaciones de perfil

**User Story:** Como usuario, quiero ver el progreso y el resultado de mis acciones en el perfil, para saber si una operación está procesándose, tuvo éxito o falló.

#### Acceptance Criteria

1. WHEN el `Profile_System` inicia una operación asíncrona de perfil (guardado de datos personales, cambio de contraseña o actualización de foto), THE `Perfil_UI` SHALL mostrar el indicador de carga ANTES de iniciar esa operación asíncrona y SHALL deshabilitar el control de acción correspondiente.
2. WHILE una operación de perfil está en curso, THE `Perfil_UI` SHALL ignorar cualquier envío adicional del mismo control de acción hasta que esa operación finalice con éxito o con error.
3. WHEN el `Profile_System` devuelve un resultado de éxito, THE `Perfil_UI` SHALL ocultar el indicador de carga y volver a habilitar el control de acción.
4. IF el `Profile_System` devuelve un resultado de error, THEN THE `Perfil_UI` SHALL ocultar el indicador de carga, volver a habilitar el control de acción, mostrar el mensaje de error correspondiente y conservar los datos ya editados en el formulario para permitir un nuevo intento.
5. IF una operación de perfil no produce respuesta del `Profile_System` en un plazo de 30 segundos, THEN THE `Perfil_UI` SHALL ocultar el indicador de carga, mostrar un mensaje de error de tiempo de espera agotado y volver a habilitar el control de acción.
6. WHEN el dominio devuelve un `Auth_Error` tipado (incluidos `USERNAME_TAKEN`, `EMAIL_TAKEN`, `INVALID_CREDENTIALS`, `PERSISTENCE_ERROR`, `VALIDATION_ERROR`, `SESSION_UNAVAILABLE` y `SESSION_CLEAR_FAILED`), THE `Perfil_UI` SHALL traducir el error tipado a un mensaje localizado, desacoplando la lógica de dominio de los textos de UI.
7. THE `Profile_System` SHALL ejecutar TODAS las operaciones de perfil (carga, guardado de datos personales, cambio de contraseña y actualización de foto) fuera del hilo principal, usando el mecanismo asíncrono ya empleado por el proyecto (el executor de Room o los repositorios existentes), y SHALL NOT bloquear el hilo de UI.

### Requirement 14: Retención de estado y eventos de un solo consumo

**User Story:** Como usuario, quiero que la app no repita navegaciones ni mensajes al girar la pantalla, para tener una experiencia coherente ante cambios de configuración.

#### Acceptance Criteria

1. THE `PerfilViewModel` SHALL exponer los resultados de éxito y de error de las operaciones de perfil, y el evento de cierre de sesión, como eventos que se entregan exactamente una vez a un único observador, de modo que un evento ya consumido no vuelva a entregarse.
2. WHEN ocurre un cambio de configuración (por ejemplo, rotación de pantalla) tras consumir un evento de éxito, de error o de cierre de sesión, THE `PerfilViewModel` SHALL NOT volver a disparar la navegación ni re-mostrar los mensajes ya consumidos.
3. IF existe un evento pendiente que aún no ha sido consumido cuando ocurre un cambio de configuración, THEN THE `PerfilViewModel` SHALL conservar ese evento y SHALL entregarlo exactamente una vez tras el cambio de configuración, sin perderlo ni duplicarlo.
4. WHEN ocurre un cambio de configuración, THE `PerfilViewModel` SHALL retener el estado de carga y SHALL restaurar el estado del formulario y la vista previa de la `Profile_Photo` seleccionada con los mismos valores que tenían antes del cambio de configuración.

### Requirement 15: Validación centralizada y reutilizable del perfil

**User Story:** Como equipo de desarrollo, quiero que las reglas de validación del perfil estén centralizadas y reutilicen las del registro, para no duplicar lógica.

#### Acceptance Criteria

1. THE `Profile_System` SHALL alojar las reglas de validación de los campos editables del perfil (nombre de usuario, correo, nueva contraseña y confirmación) en un único componente de validación reutilizable del dominio (`Profile_Validator`).
2. THE `Profile_Validator` SHALL reutilizar las mismas reglas idénticas definidas para el registro (`Registration_Validator`) en los campos comunes, incluida la regla común de longitud máxima de 64 caracteres para la contraseña, de modo que un mismo valor produzca el mismo resultado de validación en registro y en perfil. La regla de longitud máxima de 64 caracteres se incorpora al `Registration_Validator` compartido (no solo en perfil), preservando la paridad entre registro y perfil.
3. THE `Profile_System` SHALL NOT dispersar las reglas de validación en la capa de UI, de modo que las mismas reglas se apliquen de forma consistente en registro y perfil.
4. IF `Profile_Validator` determina que uno o más campos editables son inválidos, THEN THE `Profile_System` SHALL rechazar el guardado, SHALL identificar cada campo inválido en el resultado de validación y SHALL NOT persistir ningún cambio en `Local_Database`.

### Requirement 16: Verificación y pruebas automatizadas

**User Story:** Como equipo de desarrollo, quiero pruebas automatizadas que cubran validación, migración y cambio de contraseña, para garantizar la corrección y prevenir regresiones.

#### Acceptance Criteria

1. THE proyecto SHALL incluir tests unitarios puros de JVM para `Profile_Validator` que ejerciten las reglas de nombre de usuario (orden formato antes que longitud), correo, y contraseña (mínimo 8, máximo 64, mayúscula, minúscula y dígito), aprovechando el hook `isEmailPattern` para no depender del framework de Android.
2. THE proyecto SHALL incluir un test de migración de esquema v4→v5 análogo a `MatchMigrationTest` que verifique la aplicación de `MIGRATION_4_5`, la creación de la columna `avatar_uri` y la conservación de los registros de usuario existentes.
3. THE proyecto SHALL incluir un test del caso de uso de cambio de contraseña que verifique que se comprueba la `Current_Password` contra el `Password_Credential` almacenado antes de aceptar el cambio y que la `New_Password` se re-hashea con un salt aleatorio nuevo.
4. THE test de cambio de contraseña SHALL verificar que ni la `Current_Password` ni la `New_Password` se exponen en texto plano en el resultado ni en las columnas persistidas.
