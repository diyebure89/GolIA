# Implementation Plan: Home con Bottom Navigation

## Overview

Este plan convierte el diseño aprobado en una serie de tareas de codificación incrementales para Android (Java, Clean Architecture + MVVM + Hilt + Room, paquete `com.diyebure.golia`). El orden respeta las dependencias: primero los recursos base (íconos, menú, color, strings), luego la sesión local (`PreferencesManager`), el contrato de UI común (`BasePlaceholderFragment` + `Screen`), los layouts (`activity_main`, `fragment_inicio`), los fragments, la reescritura de `MainActivity`, la integración con el login (`LoginViewModel` + `LoginActivity`) y, por último, la limpieza de la actividad obsoleta (`HomeActivity` + manifest). Los tests se ubican cerca de la implementación que validan y están marcados como opcionales con `*`.

Las tareas de test de propiedad usan **jqwik** (JVM) con mínimo 100 iteraciones y se anotan con `// Feature: home-bottom-navigation, Property {n}: {texto}`. Las propiedades que requieren `FragmentManager` real se ejecutan como pruebas instrumentadas (Espresso / `ActivityScenario` / `FragmentScenario`).

## Tasks

- [x] 1. Crear recursos base de navegación (íconos, menú, ColorStateList, strings)
  - [x] 1.1 Crear el ícono vectorial de Noticias
    - Crear `app/src/main/res/drawable/ic_noticias.xml` como `VectorDrawable` monocromo (`#FFFFFFFF`, tintable) con forma de periódico
    - Reutilizar los vectoriales existentes `ic_home.xml`, `ic_partidos.xml`, `ic_ranking.xml`, `ic_perfil.xml` (no crear nuevos)
    - _Requirements: 1.3_

  - [x] 1.2 Reescribir el menú de navegación inferior a 5 destinos
    - Modificar `app/src/main/res/menu/bottom_nav_menu.xml` con 5 `item` e ids `nav_inicio`, `nav_partidos`, `nav_noticias`, `nav_ranking`, `nav_perfil`
    - Asignar íconos (`ic_home`, `ic_partidos`, `ic_noticias`, `ic_ranking`, `ic_perfil`) y títulos `@string/inicio`, `@string/partidos`, `@string/noticias`, `@string/ranking`, `@string/perfil`
    - _Requirements: 1.2, 1.3, 1.4, 10.1_

  - [x] 1.3 Reescribir el ColorStateList de los ítems de navegación
    - Modificar `app/src/main/res/color/nav_item_color.xml`: `state_checked=true -> @color/blue_end` (Color_Activo) y color por defecto `@color/text_gray` (Color_Inactivo)
    - _Requirements: 4.1, 4.2, 4.3, 10.2_

  - [x] 1.4 Añadir las cadenas nuevas de la pantalla Inicio
    - Añadir en `app/src/main/res/values/strings.xml`: `inicio_bienvenida_formato` ("BIENVENIDO %1$s"), `ver_todos`, `partidos_proximos`, `cd_notificaciones`, `stat_pronosticos`, `stat_aciertos`, `stat_puntos`, `stat_ranking`
    - Verificar que existen `inicio`, `partidos`, `noticias`, `ranking`, `perfil`; añadir las que falten
    - _Requirements: 1.4, 7.2, 7.3, 7.4_

- [x] 2. Extender la sesión local con el nombre de usuario
  - [x] 2.1 Añadir persistencia del nombre en PreferencesManager
    - Modificar `data/local/PreferencesManager.java`: añadir constante `KEY_USER_NAME` y los métodos `saveUserName(String)` (cifra con `encrypt`) y `getUserName()` (descifra con `decrypt`, devuelve `null` si no hay)
    - Añadir `.remove(KEY_USER_NAME)` dentro de `clearTokens()` para no dejar nombre residual tras logout
    - _Requirements: 11.1, 11.2, 11.3_

  - [ ]* 2.2 Escribir prueba de propiedad de round-trip del nombre (instrumentada)
    - **Property 6: Round-trip de persistencia del nombre** — para cualquier nombre, `saveUserName` seguido de `getUserName` devuelve un valor equivalente
    - Ejecutar como test instrumentado (dependencia de `SharedPreferences` + Keystore) o con un doble/fake JVM del almacenamiento; jqwik con 100 iteraciones
    - **Validates: Requirements 11.1**

- [x] 3. Crear el contrato de UI común y el enum de destinos
  - [x] 3.1 Crear BasePlaceholderFragment
    - Crear `presentation/ui/common/BasePlaceholderFragment.java` como clase abstracta que extiende `Fragment`; en `onViewCreated` llama a `super` y luego a `bindPlaceholderData()`
    - Declarar el método abstracto `protected abstract void bindPlaceholderData();`
    - _Requirements: 8.1, 8.2, 8.4_

  - [x] 3.2 Reescribir el enum Screen a 5 destinos con FragmentFactory
    - Reescribir `presentation/ui/common/Screen.java` con `INICIO/PARTIDOS/NOTICIAS/RANKING/PERFIL`, cada uno con `menuItemId` (`R.id.nav_*`), `tag` estable (`"fragment_inicio"`, `"fragment_partidos"`, `"fragment_noticias"`, `"fragment_ranking"`, `"fragment_perfil"`) y `FragmentFactory` (sin reflexión)
    - Exponer `getMenuItemId()`, `getTag()`, `newFragment()`, `fromMenuId(int)` (devuelve `null` para ids desconocidos) y `defaultScreen()`
    - _Requirements: 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.8_

  - [ ]* 3.3 Escribir prueba de propiedad/unidad del mapeo de Screen (JVM)
    - **Property 1 (base de mapeo):** para todo `Screen s`, `Screen.fromMenuId(s.getMenuItemId()) == s`; `values().length == 5`; tags únicos y no nulos; `fromMenuId(id_desconocido) == null`
    - jqwik con 100 iteraciones sobre el conjunto de destinos e ids arbitrarios
    - **Validates: Requirements 1.2, 2.6**

  - [ ]* 3.4 Escribir prueba del contrato de poblado (instrumentada)
    - **Property 8: El contrato de poblado se invoca una vez por creación de vista** — una subclase de prueba de `BasePlaceholderFragment` verifica que `bindPlaceholderData()` se llamó exactamente una vez tras `onViewCreated` (contador)
    - Ejecutar con Robolectric o `FragmentScenario`
    - **Validates: Requirements 8.1**

- [x] 4. Checkpoint - Asegurar que el proyecto compila y los tests pasan
  - Asegúrate de que todos los tests pasan; pregunta al usuario si surgen dudas.

- [x] 5. Rediseñar los layouts de la pantalla principal e Inicio
  - [x] 5.1 Ajustar activity_main.xml
    - Modificar `res/layout/activity_main.xml`: `CoordinatorLayout` con `background=@color/blue_dark`; `FragmentContainerView` id `fragment_container` (fondo `@color/blue_dark`, `layout_marginBottom=?attr/actionBarSize`); `BottomNavigationView` id `bottom_navigation` (`background=@color/blue_dark`, `itemIconTint`/`itemTextColor=@color/nav_item_color`, `menu=@menu/bottom_nav_menu`, `labelVisibilityMode=labeled`)
    - _Requirements: 1.1, 4.1, 4.2, 5.1, 5.2, 5.3, 10.2_

  - [x] 5.2 Rediseñar fragment_inicio.xml según el mockup
    - Modificar `res/layout/fragment_inicio.xml` con fondo transparente: encabezado GOL-IA + `ImageView` `icon_notifications` (`clickable=true`, `contentDescription=@string/cd_notificaciones`); tarjeta de bienvenida con `text_welcome`; cuatro estadísticas independientes con valores `text_stat_pronosticos_value`, `text_stat_aciertos_value`, `text_stat_puntos_value`, `text_stat_ranking_value` (etiquetas fijas Pronóst./Aciertos/Puntos/Ranking); sección "Partidos próximos" con enlace "Ver todos"; tarjetas de partido `card_match_1` y `card_match_2` con equipos y cuotas
    - Reemplazar el `TextView` de estadísticas concatenadas (`text_stats`) por los cuatro bloques independientes
    - _Requirements: 5.2, 7.1, 7.3, 7.4, 7.5, 7.7_

- [x] 6. Adaptar los fragments al contrato común
  - [x] 6.1 Migrar los cuatro fragments placeholder a BasePlaceholderFragment
    - Modificar `PartidosFragment.java`, `NoticiasFragment.java`, `RankingFragment.java`, `PerfilFragment.java` para extender `BasePlaceholderFragment` e implementar `bindPlaceholderData()` conservando su placeholder actual
    - _Requirements: 8.1, 8.2, 8.4_

  - [x] 6.2 Reescribir InicioFragment con datos de sesión y placeholders
    - Modificar `InicioFragment.java` para extender `BasePlaceholderFragment`; en `bindPlaceholderData()` leer el nombre desde `PreferencesManager` (solo sesión local), aplicar `resolveName(String)` (método estático puro con fallback `"Invitado"`), poblar `text_welcome` con `R.string.inicio_bienvenida_formato`, fijar las 4 estadísticas placeholder y hacer `icon_notifications` clickable sin acción
    - Exponer `resolveName(String)` como método estático puro (para test JVM)
    - _Requirements: 7.1, 7.2, 7.3, 7.5, 7.6, 8.1, 11.2, 11.3_

  - [ ]* 6.3 Escribir prueba de propiedad de resolveName (JVM)
    - **Property 5: Resolución del nombre con fallback** — para cualquier cadena, `resolveName` devuelve la cadena si no es nula ni solo espacios, y el valor por defecto en caso contrario
    - jqwik con 100 iteraciones (incluir `null`, vacías, solo espacios, con contenido)
    - **Validates: Requirements 7.2, 11.3**

- [x] 7. Reescribir MainActivity como host de navegación
  - [x] 7.1 Implementar la navegación show/hide por tag y el estado seleccionado
    - Reescribir `MainActivity.java`: `setContentView(R.layout.activity_main)`, obtener `bottom_navigation`, `setOnItemSelectedListener`; Inicio por defecto si `savedInstanceState == null`; restauración por tag (`restoreVisibleFragment`) si `!= null`; `selectDestination` con `add` por tag + `show`/`hide` (nunca `replace`/`addToBackStack`); reselección devuelve `true` sin recrear
    - Guardar `selectedMenuId` en `onSaveInstanceState` (`KEY_SELECTED`) y restaurarlo con fallback `nav_inicio`
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.1, 3.2, 4.4, 6.1, 6.2, 6.3, 6.5, 9.1_

  - [x] 7.2 Implementar el manejo del botón atrás con OnBackPressedCallback
    - Añadir `OnBackPressedCallback` en `MainActivity`: si `selectedMenuId != nav_inicio`, `setSelectedItemId(nav_inicio)`; si es Inicio, delegar salida por defecto del sistema
    - _Requirements: 9.2, 9.3, 9.4_

  - [ ]* 7.3 Escribir pruebas de propiedad instrumentadas de navegación (Espresso / ActivityScenario)
    - **Property 1: Selección de destino muestra el fragment correcto y exactamente uno visible** — para cada destino, seleccionar y verificar fragment por tag `isVisible()` y los demás `isHidden()` (exactamente uno visible). **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.4**
    - **Property 2: Idempotencia de reselección** — reseleccionar el destino actual conserva la misma instancia y no añade otra. **Validates: Requirements 2.7, 9.1**
    - **Property 3: Reutilización de instancia por etiqueta** — secuencia A→B→A devuelve la misma instancia por tag. **Validates: Requirements 2.8**
    - Instrumentadas; parametrizadas sobre el conjunto finito de destinos

  - [ ]* 7.4 Escribir prueba de propiedad instrumentada de rotación (ActivityScenario.recreate)
    - **Property 4: Round-trip de rotación preserva el destino** — para cada destino, `recreate()` preserva el fragment visible y `selectedItemId`; un destino distinto de Inicio no se reinicia a Inicio
    - Instrumentada
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**

  - [ ]* 7.5 Escribir prueba de propiedad instrumentada del botón atrás
    - **Property 7: El botón atrás desde un destino distinto de Inicio lleva a Inicio** — para cada destino != Inicio, `pressBack()` deja Inicio visible y seleccionado; en Inicio, `pressBack()` delega la salida
    - Instrumentada
    - **Validates: Requirements 9.3, 9.4**

  - [ ]* 7.6 Escribir tests de ejemplo instrumentados de estructura y contenido
    - Verificar presencia de `fragment_container` y `bottom_navigation`; 5 items con títulos correctos; arranque por defecto en Inicio; encabezado con campana clickable; 4 estadísticas visibles; sección "Partidos próximos" + "Ver todos"; entre 1 y 10 tarjetas de partido
    - Instrumentados (Espresso)
    - _Requirements: 1.1, 1.2, 3.1, 3.2, 7.1, 7.3, 7.4, 7.5_

- [x] 8. Checkpoint - Asegurar que la navegación compila y los tests pasan
  - Asegúrate de que todos los tests pasan; pregunta al usuario si surgen dudas.

- [x] 9. Integrar el login con la nueva sesión y host
  - [x] 9.1 Guardar el nombre de usuario al iniciar sesión
    - Modificar `ui/auth/LoginViewModel.java`: en el éxito de login, tras `saveUserId`, llamar a `preferencesManager.saveUserName(user.getFullName())` solo si `user != null`
    - _Requirements: 11.1_

  - [x] 9.2 Apuntar la navegación post-login a MainActivity
    - Modificar `LoginActivity.java`: `navigateToHome()` lanza `MainActivity` (no `HomeActivity`), conservando los flags `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`
    - _Requirements: 2.1, 3.1_

- [x] 10. Limpiar la actividad obsoleta
  - [x] 10.1 Eliminar HomeActivity y su layout, y actualizar el manifest
    - Eliminar `HomeActivity.java` y `res/layout/activity_home.xml`
    - Quitar la entrada `<activity android:name=".HomeActivity" .../>` de `AndroidManifest.xml` y verificar que `MainActivity` está declarada; no modificar el launcher (Splash/Login permanece igual)
    - _Requirements: 2.1, 3.1_

- [x] 11. Checkpoint final - Asegurar que todo compila y los tests pasan
  - Asegúrate de que todos los tests pasan; pregunta al usuario si surgen dudas.

## Notes

- Las tareas marcadas con `*` son opcionales (tests) y pueden omitirse para un MVP más rápido; el agente NO debe implementarlas salvo indicación.
- Las pruebas instrumentadas requieren `FragmentManager` real (Espresso / `ActivityScenario` / `FragmentScenario` / Robolectric) y están señaladas explícitamente.
- Las pruebas de propiedad usan **jqwik** con mínimo 100 iteraciones y se anotan con `// Feature: home-bottom-navigation, Property {n}: {texto}`.
- Cada tarea referencia requisitos concretos para trazabilidad; las tareas de test referencian su propiedad de diseño.
- Las verificaciones de estilo visual (R5) y de configuración (R1.3, R4.3, R10.2, R10.3) se cubren con smoke tests / aserciones de recursos, no con PBT.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4", "2.1", "3.1"] },
    { "id": 1, "tasks": ["2.2", "3.2"] },
    { "id": 2, "tasks": ["3.3", "3.4", "5.1", "5.2", "6.1"] },
    { "id": 3, "tasks": ["6.2", "9.1", "9.2"] },
    { "id": 4, "tasks": ["6.3", "7.1"] },
    { "id": 5, "tasks": ["7.2", "10.1"] },
    { "id": 6, "tasks": ["7.3", "7.4", "7.5", "7.6"] }
  ]
}
```
