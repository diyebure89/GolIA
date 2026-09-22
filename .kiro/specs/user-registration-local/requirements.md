# Requirements Document

## Introduction

Esta funcionalidad da vida a la pantalla de registro de usuario de la app GolIA (predicciones de fútbol, Android, Java, Clean Architecture + MVVM + Hilt + Room). El alcance es un MVP donde tanto el registro como el inicio de sesión funcionan por completo contra la base de datos local (Room/SQLite), sin backend remoto ni verificación de cuenta por correo electrónico.

El sistema recopila los datos del usuario en la pantalla de registro, los valida campo por campo, garantiza la unicidad del nombre de usuario y del correo electrónico, almacena la contraseña únicamente como un hash con salt, y persiste al usuario en la base de datos local. Tras un registro exitoso, el usuario NO inicia sesión automáticamente: se le redirige a la pantalla de inicio de sesión para que introduzca sus credenciales. El inicio de sesión valida las credenciales contra esa misma base de datos local y, cuando tiene éxito, navega a la pantalla principal. La interfaz refleja los estados de carga, éxito y error.

Nota sobre el motor de base de datos: este proyecto persiste localmente con Room (SQLite) en el dispositivo Android, detrás de una interfaz de repositorio del dominio. No se contempla ni se usará MySQL; la abstracción por repositorio se mantiene por buenas prácticas de arquitectura (testabilidad y aislamiento del dominio), no como preparación para una migración a MySQL.

## Glossary

- **Registration_System**: Componente del dominio y datos que valida y persiste un nuevo usuario en la base de datos local (RegisterUseCase + AuthRepository + UserDao/Room).
- **Login_System**: Componente que valida las credenciales de un usuario contra la base de datos local (LoginUseCase + AuthRepository + UserDao/Room).
- **Registro_UI**: La pantalla RegistroActivity y su RegisterViewModel, responsables de recibir la entrada del usuario, mostrar mensajes de validación y reflejar estados de carga/éxito/error. Tras un registro exitoso navega a Login_UI y no inicia sesión automáticamente.
- **Login_UI**: La pantalla LoginActivity y su LoginViewModel, responsables de recibir las credenciales, reflejar estados de carga/éxito/error y navegar a Home_Screen tras un inicio de sesión exitoso.
- **Password_Hasher**: Componente que transforma una contraseña en un hash con salt para su almacenamiento y verificación, sin exponer la contraseña en texto plano. Utiliza PBKDF2WithHmacSHA256, disponible de forma nativa en Android sin dependencias externas.
- **Local_Database**: La base de datos local del dispositivo gestionada por Room (motor SQLite en Android) que contiene la tabla de usuarios (UserEntity/UserDao). El acceso se realiza a través de la interfaz de repositorio del dominio. El proyecto no usa MySQL.
- **Full_Name**: Nombre completo del usuario (ejemplo: "Carlos Rodríguez").
- **Username**: Nombre de usuario OPCIONAL del usuario (ejemplo: "carlosgol"). Puede omitirse; si se proporciona, se normaliza a minúsculas y debe ser único entre los valores no nulos. Cuando no se proporciona, se persiste como NULL (nunca como cadena vacía).
- **Email**: Correo electrónico único del usuario (ejemplo: "carlos@ejemplo.com"). Se normaliza a minúsculas antes de validar unicidad y de persistir.
- **Password**: Contraseña en texto plano introducida por el usuario. Debe tener al menos 8 caracteres e incluir al menos una letra mayúscula, una letra minúscula y un número.
- **Confirm_Password**: Segunda entrada de contraseña que debe coincidir con Password.
- **Home_Screen**: La pantalla principal de la app a la que se navega tras un inicio de sesión exitoso.
- **Auth_Error**: Conjunto tipado de errores de autenticación del dominio (USERNAME_TAKEN, EMAIL_TAKEN, INVALID_CREDENTIALS, PERSISTENCE_ERROR, VALIDATION_ERROR) que la capa de UI traduce a mensajes localizados.
- **MVVM (Model-View-ViewModel)**: Patrón de arquitectura de presentación que separa la vista (Activity/Fragment) de la lógica de presentación (ViewModel) y de los datos (Model). El ViewModel expone el estado de la interfaz como observables (LiveData) y sobrevive a los cambios de configuración, evitando que la lógica resida en la vista.
- **Hilt**: Framework de inyección de dependencias para Android, construido sobre Dagger. Provee automáticamente las dependencias de la app (repositorios, DAOs, use cases, ViewModels) mediante anotaciones (por ejemplo, @Inject, @HiltViewModel, @Module, @Binds), eliminando la construcción manual de objetos y facilitando el desacoplamiento y las pruebas.

## Requirements

### Requirement 1: Validación del campo Nombre completo

**User Story:** Como usuario nuevo, quiero que se valide el nombre completo que introduzco, para asegurar que mi perfil tenga un nombre válido.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de registro con Full_Name vacío o compuesto únicamente por espacios en blanco, THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_nombre indicando que el nombre completo es obligatorio, y SHALL rechazar el envío del formulario.
2. WHEN el usuario envía el formulario de registro con un Full_Name que contiene menos de 3 caracteres tras eliminar espacios iniciales y finales, THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_nombre indicando que el nombre completo debe tener al menos 3 caracteres, y SHALL rechazar el envío del formulario.
3. IF el Full_Name contiene más de 50 caracteres tras eliminar espacios iniciales y finales, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_nombre indicando que el nombre completo no debe superar los 50 caracteres, y SHALL rechazar el envío del formulario.
4. WHEN el usuario envía el formulario de registro con un Full_Name de 3 a 50 caracteres (ambos inclusive) tras eliminar espacios iniciales y finales, THE Registro_UI SHALL aceptar el valor de Full_Name como válido.

### Requirement 2: Validación del campo Nombre de usuario

**User Story:** Como usuario nuevo, quiero que el nombre de usuario sea opcional y con un formato bien definido, para poder registrarme rápidamente y con un identificador predecible.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de registro con Username vacío o compuesto únicamente por espacios en blanco, THE Registro_UI SHALL aceptar el envío del formulario sin mostrar error en textInputLayout_usuario, tratando el Username como no proporcionado.
2. IF el Username proporcionado (tras eliminar espacios iniciales y finales y tras retirar el símbolo '@' inicial si estuviera presente) contiene menos de 3 caracteres, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_usuario indicando que el nombre de usuario debe tener al menos 3 caracteres, y SHALL rechazar el envío del formulario.
3. IF el Username proporcionado (tras eliminar espacios iniciales y finales y tras retirar el símbolo '@' inicial si estuviera presente) contiene más de 30 caracteres, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_usuario indicando que el nombre de usuario no debe superar los 30 caracteres, y SHALL rechazar el envío del formulario.
4. WHEN el usuario proporciona un Username no vacío, THE Registro_UI SHALL aceptar únicamente caracteres del conjunto [A-Za-z0-9_.], permitiendo un símbolo '@' opcional solo como primer carácter; THE Registration_System SHALL almacenar el Username sin el '@' inicial si estuviera presente; IF el Username contiene espacios internos o cualquier carácter fuera del conjunto permitido, THEN THE Registro_UI SHALL rechazar el envío del formulario y mostrar un error de formato en textInputLayout_usuario.

### Requirement 3: Validación del campo Correo electrónico

**User Story:** Como usuario nuevo, quiero que se valide mi correo electrónico, para evitar registrar una dirección con formato incorrecto.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de registro con Email vacío, THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_email indicando que el correo electrónico es obligatorio.
2. IF el Email introducido no cumple el patrón android.util.Patterns.EMAIL_ADDRESS, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_email indicando que el formato del correo electrónico es inválido.
3. WHEN el usuario envía el formulario de registro con un Email que cumple el patrón android.util.Patterns.EMAIL_ADDRESS, THE Registro_UI SHALL aceptar el valor de Email como válido.

### Requirement 4: Validación del campo Contraseña

**User Story:** Como usuario nuevo, quiero que se valide mi contraseña con requisitos de complejidad, para asegurar que sea suficientemente segura.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de registro con Password vacío, THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_password indicando que la contraseña es obligatoria, y SHALL rechazar el envío del formulario.
2. WHEN el usuario envía el formulario de registro con un Password que contiene menos de 8 caracteres, THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_password indicando que la contraseña debe tener al menos 8 caracteres, y SHALL rechazar el envío del formulario.
3. IF el Password no contiene al menos una letra mayúscula, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_password indicando que la contraseña debe contener al menos una letra mayúscula, y SHALL rechazar el envío del formulario.
4. IF el Password no contiene al menos una letra minúscula, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_password indicando que la contraseña debe contener al menos una letra minúscula, y SHALL rechazar el envío del formulario.
5. IF el Password no contiene al menos un dígito numérico, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_password indicando que la contraseña debe contener al menos un número, y SHALL rechazar el envío del formulario.
6. WHEN el usuario envía el formulario de registro con un Password de 8 o más caracteres que contiene al menos una mayúscula, al menos una minúscula y al menos un número, THE Registro_UI SHALL aceptar el valor de Password como válido.

### Requirement 5: Validación de Confirmar contraseña

**User Story:** Como usuario nuevo, quiero confirmar mi contraseña, para evitar registrarme con una contraseña que escribí por error.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de registro con Confirm_Password vacío, THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_confirmar_password indicando que la confirmación de contraseña es obligatoria, y SHALL rechazar el envío del formulario.
2. IF Confirm_Password no está vacío y su valor no coincide exactamente, carácter por carácter, con el valor de Password, THEN THE Registro_UI SHALL mostrar un mensaje de error en textInputLayout_confirmar_password indicando que las contraseñas no coinciden, y SHALL rechazar el envío del formulario.
3. WHEN Confirm_Password no está vacío y su valor coincide exactamente, carácter por carácter, con el valor de Password, THE Registro_UI SHALL aceptar la confirmación como válida.

### Requirement 6: Hash de contraseña con salt

**User Story:** Como usuario, quiero que mi contraseña se almacene de forma segura, para que nadie con acceso a la base de datos pueda leerla.

#### Acceptance Criteria

1. WHEN el Registration_System procesa un registro con validaciones superadas, THE Password_Hasher SHALL derivar el hash usando PBKDF2WithHmacSHA256 (disponible de forma nativa en Android, sin dependencias externas), con un salt aleatorio de 16 bytes generado con SecureRandom, un mínimo de 120000 iteraciones y una longitud de clave derivada de 256 bits.
2. WHEN el Registration_System persiste un usuario, THE Local_Database SHALL almacenar la credencial en un formato que incluya el algoritmo, el número de iteraciones, el salt (Base64) y el hash derivado (Base64), de modo que la verificación no dependa de parámetros embebidos en el código, y SHALL NOT almacenar la Password en texto plano en ningún campo.
3. WHEN el Login_System verifica credenciales, THE Password_Hasher SHALL recalcular el hash de la Password introducida usando el algoritmo, las iteraciones y el salt almacenados en la credencial.
4. WHEN el Password_Hasher compara el hash recalculado con el hash almacenado, THE Password_Hasher SHALL usar una comparación en tiempo constante (por ejemplo, MessageDigest.isEqual).
5. WHEN el hash recalculado de la Password introducida coincide con el hash almacenado, THE Login_System SHALL considerar la autenticación como exitosa.
6. IF el hash recalculado de la Password introducida no coincide con el hash almacenado, THEN THE Login_System SHALL rechazar la autenticación con un error de credenciales inválidas sin revelar si falló el identificador o la contraseña.

### Requirement 7: Unicidad y normalización de nombre de usuario y correo

**User Story:** Como sistema, quiero garantizar que cada nombre de usuario y cada correo sean únicos y estén normalizados, para evitar cuentas duplicadas o ambiguas.

#### Acceptance Criteria

1. WHEN el Registration_System procesa un registro, THE Registration_System SHALL normalizar Email y Username a minúsculas (trim + toLowerCase) antes de validar unicidad y antes de persistir.
2. IF el Username normalizado enviado ya existe en Local_Database entre los valores no nulos, THEN THE Registration_System SHALL rechazar el registro y devolver un error USERNAME_TAKEN indicando que el nombre de usuario ya está en uso.
3. IF el Email normalizado enviado ya existe en Local_Database, THEN THE Registration_System SHALL rechazar el registro y devolver un error EMAIL_TAKEN indicando que el correo electrónico ya está registrado.
4. THE Local_Database SHALL definir el Username y el Email como columnas con restricción de unicidad en la tabla de usuarios sobre los valores ya normalizados, aplicando la unicidad del Username únicamente a valores no nulos.
5. IF la escritura en Local_Database viola la restricción de unicidad, THEN THE Registration_System SHALL devolver un error de conflicto y SHALL NOT persistir el usuario en conflicto.

### Requirement 8: Persistencia del registro en la base de datos

**User Story:** Como usuario nuevo, quiero que mi cuenta se guarde de forma persistente, para poder iniciar sesión después.

#### Acceptance Criteria

1. THE Local_Database SHALL usar un identificador único de tipo UUID (versión 4) como clave primaria del usuario.
2. WHEN el Registration_System recibe un registro con todas las validaciones superadas y sin conflictos de unicidad, THE Local_Database SHALL persistir un único registro de usuario con Full_Name, Username normalizado (si fue proporcionado), Email normalizado, la credencial de contraseña (algoritmo, iteraciones, salt y hash) y una marca de tiempo de creación en UTC.
3. WHEN el Username no fue proporcionado, THE Local_Database SHALL persistir NULL en la columna de nombre de usuario (nunca cadena vacía), y la restricción de unicidad del nombre de usuario SHALL aplicar únicamente a valores no nulos.
4. WHEN el Local_Database persiste el nuevo usuario correctamente, THE Registration_System SHALL devolver un resultado de éxito que incluya el identificador único del usuario creado.
5. IF ocurre un error al escribir en Local_Database durante el registro, THEN THE Registration_System SHALL devolver un resultado de error PERSISTENCE_ERROR describiendo el fallo de persistencia, y SHALL NOT dejar datos parciales del usuario en Local_Database.
6. THE Registration_System SHALL acceder a la persistencia únicamente a través de la interfaz de repositorio del dominio, de modo que la implementación de almacenamiento quede aislada del dominio y de la presentación.

### Requirement 9: Inicio de sesión contra la base de datos local

**User Story:** Como usuario registrado, quiero iniciar sesión con mis credenciales, para acceder a mi cuenta en la app.

#### Acceptance Criteria

1. WHEN el usuario envía el formulario de login con un identificador y una Password, THE Login_System SHALL normalizar el identificador introducido a minúsculas (trim + toLowerCase) antes de buscar en Local_Database.
2. WHEN el Login_System busca al usuario, THE Login_System SHALL buscar en Local_Database un usuario cuyo Username o Email coincida con el identificador normalizado.
3. IF no existe en Local_Database ningún usuario que coincida con el identificador normalizado, THEN THE Login_System SHALL devolver un error INVALID_CREDENTIALS de credenciales inválidas.
4. IF existe un usuario coincidente pero el hash recalculado de la Password introducida no coincide con el hash almacenado, THEN THE Login_System SHALL devolver un error INVALID_CREDENTIALS de credenciales inválidas.
5. WHEN existe un usuario coincidente y el hash recalculado de la Password coincide con el hash almacenado, THE Login_System SHALL devolver un resultado de éxito con la identidad del usuario autenticado.

### Requirement 10: Estados de la interfaz durante registro e inicio de sesión

**User Story:** Como usuario, quiero ver el progreso y el resultado de mis acciones, para saber si el registro o el login están procesándose, tuvieron éxito o fallaron.

#### Acceptance Criteria

1. WHILE el Registration_System procesa una solicitud de registro, THE Registro_UI SHALL mostrar progressBar_registro y deshabilitar button_registro, ignorando cualquier envío adicional del formulario.
2. WHEN el Registration_System devuelve un resultado de éxito, THE Registro_UI SHALL ocultar progressBar_registro y mostrar la confirmación de registro exitoso mediante un Toast.
3. IF el Registration_System devuelve un resultado de error, THEN THE Registro_UI SHALL ocultar progressBar_registro, volver a habilitar button_registro y mostrar el mensaje de error correspondiente.
4. WHILE el Login_System procesa una solicitud de inicio de sesión, THE Login_UI SHALL mostrar un indicador de carga y deshabilitar el botón de inicio de sesión, ignorando cualquier envío adicional del formulario.
5. IF el Login_System devuelve un resultado de error, THEN THE Login_UI SHALL ocultar el indicador de carga, volver a habilitar el botón de inicio de sesión y mostrar el error de credenciales inválidas como un mensaje global (por ejemplo, un Toast), no asociado a un campo de entrada específico, para no revelar si falló el identificador o la contraseña.

### Requirement 11: Navegación desde registro e inicio de sesión

**User Story:** Como usuario, quiero moverme entre las pantallas de registro, login y principal, para completar mi flujo de acceso a la app.

#### Acceptance Criteria

1. WHEN el Registration_System devuelve un resultado de éxito, THE Registro_UI SHALL navegar a la pantalla de inicio de sesión (Login_UI) sin iniciar sesión automáticamente.
2. WHEN el Login_System devuelve un resultado de éxito, THE Login_UI SHALL navegar a Home_Screen.
3. WHEN el usuario selecciona textView_iniciar_sesion dentro de linearLayout_yacuenta, THE Registro_UI SHALL navegar a la pantalla de inicio de sesión.
4. WHEN el usuario selecciona button2, THE Registro_UI SHALL regresar a la pantalla anterior.

### Requirement 12: Indicación visual de campos obligatorios y opcionales

**User Story:** Como usuario nuevo, quiero ver claramente qué campos del formulario de registro son obligatorios y cuál es opcional, para completar el registro correctamente sin adivinar.

#### Acceptance Criteria

1. THE Registro_UI SHALL mostrar en la etiqueta de cada campo obligatorio (Nombre completo, Correo electrónico, Contraseña y Confirmar contraseña) un indicador visual de obligatoriedad, específicamente un asterisco ("*").
2. THE Registro_UI SHALL mostrar en la etiqueta del campo Nombre de usuario un indicador de que es opcional, específicamente el texto "(opcional)".
3. THE Registro_UI SHALL mostrar en la pantalla de registro una leyenda que explique que el asterisco indica campos obligatorios.
4. WHERE se muestre el indicador de obligatoriedad (asterisco), THE Registro_UI SHALL presentarlo en color rojo, con contraste suficiente para su lectura sobre el fondo oscuro de la pantalla.

### Requirement 13: Retención de estado y eventos de un solo consumo

**User Story:** Como usuario, quiero que la app no repita navegaciones ni mensajes al girar la pantalla, para tener una experiencia coherente ante cambios de configuración.

#### Acceptance Criteria

1. THE RegisterViewModel y THE LoginViewModel SHALL exponer los eventos de éxito y de error como eventos de un solo consumo (single-use events).
2. WHEN ocurre un cambio de configuración (por ejemplo, rotación de pantalla) tras consumir un evento de éxito o de error, THE RegisterViewModel y THE LoginViewModel SHALL NOT volver a disparar la navegación ni re-mostrar los mensajes ya consumidos.
3. WHEN ocurre un cambio de configuración, THE RegisterViewModel y THE LoginViewModel SHALL retener el estado del formulario y el estado de carga.

### Requirement 14: Validación centralizada y reutilizable

**User Story:** Como equipo de desarrollo, quiero que las reglas de validación estén centralizadas, para reutilizarlas en distintos flujos sin duplicar lógica.

#### Acceptance Criteria

1. THE Registration_System SHALL alojar las reglas de validación de los campos de registro (nombre completo, nombre de usuario, correo, contraseña, confirmación) en un componente de validación reutilizable del dominio.
2. THE Registration_System SHALL NOT dispersar las reglas de validación en la capa de UI, de modo que las mismas reglas puedan aplicarse en registro y en futuros flujos.

### Requirement 15: Contrato tipado de errores de autenticación

**User Story:** Como equipo de desarrollo, quiero un contrato tipado de errores de autenticación, para desacoplar la lógica de dominio de los textos de la interfaz.

#### Acceptance Criteria

1. THE Registration_System y THE Login_System SHALL definir un conjunto tipado de errores de autenticación que incluya al menos USERNAME_TAKEN, EMAIL_TAKEN, INVALID_CREDENTIALS, PERSISTENCE_ERROR y VALIDATION_ERROR.
2. WHEN el dominio devuelve un error tipado, THE Registro_UI y THE Login_UI SHALL traducir el error tipado a un mensaje localizado para el usuario, desacoplando la lógica de dominio de los textos de UI.
