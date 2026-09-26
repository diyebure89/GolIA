# Spike Notes — Contrato real de NewsData.io (Tarea 1)

> **Estado de ejecución del spike:** ⚠️ **No se realizó llamada real.** No hay `NEWSDATA_API_KEY`
> configurada en `local.properties` (solo existe `API_FOOTBALL_KEY`). Por tanto, **todos** los
> hallazgos de este documento están marcados como **"documentado/hipotético, pendiente de
> confirmación en vivo"**. Ningún valor de este documento fue inventado a partir de una llamada:
> proviene de la documentación pública de NewsData.io y del diseño del spec.
>
> **Nota de cumplimiento de licencia:** el contenido derivado de la documentación pública de
> NewsData.io ha sido reformulado/parafraseado por cumplimiento de licencia. Fuente:
> [NewsData.io — Documentation](https://newsdata.io/documentation).
>
> **Seguridad:** no se ha versionado ninguna API key ni se ha impreso ningún valor de secreto.

---

## 0. Cómo completar el spike cuando haya API key

1. Añadir a `local.properties` (no versionado): `NEWSDATA_API_KEY=<tu_clave>` — **nunca** commitear el valor.
2. Ejecutar una llamada real de solo lectura al endpoint `/news` del plan gratuito, p. ej.:

   ```
   GET https://newsdata.io/api/1/news?apikey=<KEY>&q=%22Premier%20League%22&language=es&category=sports
   ```

   (Sustituir `<KEY>` en el momento de ejecutar; no dejar la clave en ningún archivo versionado.)
3. Registrar el JSON de respuesta (recortando el `apikey` de cualquier log), las cabeceras de
   respuesta y el código HTTP.
4. Repetir provocando 429 (o esperar al agotamiento de cuota) para capturar la forma del error.
5. Actualizar las columnas **"Confirmado en vivo"** de las tablas siguientes y marcar en verde los
   ajustes de constantes aguas abajo.

---

## 1. Contrato del DTO de `/news`

Endpoint base: `https://newsdata.io/api/1/`, recurso `news`.

### 1.1 Forma raíz de la respuesta (éxito)

| Campo | Tipo esperado | Nulabilidad | Estado | Notas |
|---|---|---|---|---|
| `status` | string | no-null | 📄 documentado, pendiente en vivo | `"success"` en éxito. |
| `totalResults` | int | no-null | 📄 documentado, pendiente en vivo | Total estimado de artículos coincidentes. |
| `results` | array de artículos | no-null (puede venir vacío) | 📄 documentado, pendiente en vivo | Lista de artículos de la página actual. |
| `nextPage` | string | nullable | 📄 documentado, pendiente en vivo | Token de paginación por cursor; ausente/`null` cuando no hay más páginas. **Se ignora en este spec** (una sola página por refresco). |

Forma raíz asumida:

```json
{ "status": "success", "totalResults": 123, "results": [ { /* artículo */ } ], "nextPage": "..." }
```

### 1.2 Contrato del artículo (elemento de `results[]`)

| Campo DTO (nombre real proveedor) | Tipo | Nulabilidad esperada | ¿Obligatorio para NO descartar? | Estado |
|---|---|---|---|---|
| `title` | string | no-null (en la práctica) | ✅ **Sí** (R3.8/R4.5) | 📄 documentado, pendiente en vivo |
| `link` | string | no-null | ✅ **Sí** (URL del artículo, R3.8/R4.5) | 📄 documentado, pendiente en vivo |
| `description` | string | **nullable** | No | 📄 documentado, pendiente en vivo |
| `image_url` | string | **nullable** | No | 📄 documentado, pendiente en vivo |
| `source_id` | string | no-null (habitual) | No | 📄 documentado, pendiente en vivo |
| `source_name` | string | nullable/variable | No | 📄 documentado, pendiente en vivo. **A confirmar:** en algunas versiones el nombre legible viene en `source_name`; en otras solo `source_id`. Ver §1.3. |
| `pubDate` | string | no-null (habitual) | Indirectamente (si no parsea → descarte, R3.11) | 📄 documentado, pendiente en vivo |

> Nota: NewsData.io suele exponer también otros campos (`article_id`, `keywords`, `creator`,
> `content`, `country`, `category`, `language`, `pubDateTZ`, etc.). El spec **solo** mapea los
> siete de arriba; el resto se ignora en los DTOs (R3.7). El `article_id` propio del proveedor
> **no** se usa como identidad: la identidad de dominio es `SHA-256(canonicalUrl)` (R13.3, tarea 6.1).

### 1.3 Riesgo del nombre de la fuente (`source_id` vs `source_name`)

- **A confirmar en vivo:** si el plan gratuito devuelve `source_name` poblado. Si NO lo hiciera y
  solo llegara `source_id`, el `NewsMapper` (tarea 4.2) debe usar `source_id` como fallback para
  `sourceName` en lugar de dejarlo vacío.
- **Decisión provisional:** `sourceName = source_name != null ? source_name : source_id`.

---

## 2. Formato y zona de `pubDate` (R3.11)

| Aspecto | Valor asumido | Estado |
|---|---|---|
| Formato | `"yyyy-MM-dd HH:mm:ss"` (p. ej. `"2024-01-15 09:30:00"`) | 📄 documentado, pendiente en vivo |
| Zona | **UTC** | 📄 documentado, pendiente en vivo |
| Campo auxiliar | `pubDateTZ` (a menudo `"UTC"`) — no mapeado, solo referencia | 📄 documentado, pendiente en vivo |

**Implicación para `NewsMapper` (R3.11):** parsear con patrón fijo `yyyy-MM-dd HH:mm:ss` y
`ZoneOffset.UTC` (p. ej. `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)`
o `SimpleDateFormat` con `TimeZone.getTimeZone("UTC")`), convertir a epoch UTC (`long publishedAtEpochUtc`).
Si el parseo lanza excepción → **descartar** el artículo (R3.11).

**Robustez recomendada (a confirmar):** si en vivo aparecieran variantes ISO-8601 con offset
(p. ej. `2024-01-15T09:30:00Z`), el mapper debería intentar un segundo patrón antes de descartar.
Documentar el patrón exacto observado y decidir si se necesita más de un formato.

---

## 3. Consumo de crédito y paginación (R6.4)

| Aspecto | Valor asumido | Estado |
|---|---|---|
| Coste por refresco | **1 petición HTTP = 1 crédito** | 📄 documentado, pendiente en vivo |
| Paginación | Cursor por `nextPage`; **se pide una sola página por refresco** (no se pagina) | 📄 documentado, pendiente en vivo |
| Tamaño de página (plan gratuito) | ~10 artículos por respuesta | 📄 documentado, pendiente en vivo |
| Cuota diaria del proveedor | ≈200 créditos/día | 📄 documentado (reformulado por licencia), pendiente en vivo |
| Reset de cuota | 00:00 UTC | 📄 documentado, pendiente en vivo |
| Cabeceras de rate limit | **A confirmar:** presencia/nombres (p. ej. `X-RateLimit-*`, `remaining`). NewsData.io históricamente **no** garantiza estas cabeceras en el plan gratuito. | ❓ pendiente en vivo |

**Implicación para `NewsRequestBudgetManager` (R6.4):** mantener `OPERATIONAL_LIMIT = 180`
(margen sobre 200), incrementar el contador **una vez por petición** (`recordRequest()`), reset a
00:00 UTC. **A confirmar en vivo:** que 1 petición consume exactamente 1 crédito. Si el proveedor
cobrara por página o por artículo, habría que recalibrar el límite operativo efectivo.

**A confirmar:** el tamaño real de página impacta el límite de retención de 50 por liga (R6.11):
con ~10 artículos/refresco, alcanzar 50 requiere varios refrescos, lo cual es coherente con el
diseño de purga.

---

## 4. Forma del error 429 / cuota agotada (R6.10)

| Aspecto | Valor asumido | Estado |
|---|---|---|
| Código HTTP | **429 Too Many Requests** | 📄 documentado, pendiente en vivo |
| Cuerpo | `{ "status": "error", "results": { "code": "RateLimitExceeded", "message": "..." } }` (nombre de código **a confirmar**) | 📄 documentado, pendiente en vivo |

> **A confirmar en vivo:** el proveedor puede señalar el agotamiento con `status="error"` y un
> código en el cuerpo, con o sin HTTP 429. El repositorio debe tratar **HTTP 429 O** un
> `status="error"` con código de rate limit como cuota agotada.

**Implicación (R6.10):** al detectar 429 (o el código de rate limit en cuerpo), el repositorio
traduce a `NewsError.QuotaExceededError`, y `NewsRequestBudgetManager` activa el flag
`quotaExhaustedUntilResetUtc` → **modo solo-caché** hasta el reset de 00:00 UTC, con independencia
del contador local (aunque sea < 180). Banner de UI: `QUOTA`.

---

## 5. Compatibilidad `q` + `language=es` + `category=sports` (R3.4)

| Aspecto | Valor asumido | Estado |
|---|---|---|
| `q` + `language` + `category` combinables en `/news` del plan gratuito | Sí | 📄 documentado, pendiente en vivo |
| Nombre del parámetro de idioma | `language` (valor `es`) | 📄 documentado, pendiente en vivo. **Nota:** algunas guías usan `lang`; el diseño/DTO asume `language`. **Confirmar cuál acepta el endpoint.** |
| Nombre del parámetro de categoría | `category` (valor `sports`) | 📄 documentado, pendiente en vivo |
| Límite de longitud de `q` (plan gratuito) | ≤100 caracteres | 📄 documentado (reformulado por licencia), pendiente en vivo |

> **A confirmar en vivo (importante):** el requisito R3.4 y el diseño mencionan `lang=es` en prosa,
> pero el DTO/servicio Retrofit (design §NewsDataApiService) usa `@Query("language")`. Verificar en
> vivo si el endpoint espera `language` o `lang`. Si el correcto fuera `lang`, ajustar
> `NewsDataApiService` (tarea 3.1). **Provisional: `language`.**

---

## 6. Ajustes a constantes / decisiones aguas abajo (resumen accionable)

| Decisión / constante | Requisito | Valor provisional (pendiente confirmación en vivo) | Impacto si el spike lo cambia |
|---|---|---|---|
| Patrón de fecha del `NewsMapper` | R3.11 | `yyyy-MM-dd HH:mm:ss` en `ZoneOffset.UTC`; no parseable → descarte | Cambiar patrón/zona o añadir patrón ISO-8601 de respaldo (tarea 4.2) |
| Campos obligatorios para no descartar | R3.7/R3.8/R4.5 | **`title` y `link`** | Si `link` viniera con otro nombre, ajustar DTO/`NewsMapper` (tareas 4.1/4.2) |
| Fallback de nombre de fuente | R3.7 | `source_name` con fallback a `source_id` | Confirmar si `source_name` llega poblado en plan gratuito (tarea 4.2) |
| Coste por refresco del `NewsRequestBudgetManager` | R6.4 | **1 petición = 1 crédito**, `OPERATIONAL_LIMIT = 180`, reset 00:00 UTC | Recalibrar límite si se cobra por página/artículo (tarea 6.3) |
| Mapeo de error de cuota agotada | R6.10 | HTTP 429 **o** `status="error"`+código rate-limit → `QuotaExceededError` → modo solo-caché hasta reset | Fijar código/cuerpo exactos (tarea 10.1 + `NewsError`) |
| Parámetro de idioma | R3.4 | `language=es` | Cambiar a `lang` si el endpoint lo exige (tarea 3.1) |
| Parámetro de categoría | R3.4 | `category=sports` | — |
| Paginación | R6.4 | Una sola página por refresco; `nextPage` ignorado | — |

---

## 7. Elementos pendientes de confirmar en vivo (checklist)

- [ ] Nombres y nulabilidad reales de `title`, `description`, `link`, `image_url`, `source_id`, `source_name`, `pubDate`.
- [ ] Forma raíz real: `status`, `totalResults`, `results`, `nextPage`.
- [ ] Formato/zona real de `pubDate` (¿`yyyy-MM-dd HH:mm:ss` UTC vs ISO-8601 con offset?).
- [ ] ¿1 petición = 1 crédito? Tamaño de página real. ¿Existen cabeceras `X-RateLimit-*`?
- [ ] Forma real del error 429 (código HTTP + cuerpo: `status`, `results.code`/`code`).
- [ ] ¿`q` + `language=es` + `category=sports` combinables sin exclusión mutua en plan gratuito?
- [ ] ¿El endpoint espera `language` o `lang`?
- [ ] ¿`source_name` llega poblado o hay que usar `source_id` de fallback?

---

## 8. Conclusión provisional

Con la información documentada, **los valores por defecto del diseño se mantienen sin cambios**
(patrón de fecha UTC, campos obligatorios `title`+`link`, 1 petición = 1 crédito, `OPERATIONAL_LIMIT
= 180`, 429 → modo solo-caché). Las tareas aguas abajo (2–14) pueden avanzar con estos valores por
defecto. Este documento debe **actualizarse con una llamada real** en cuanto haya una
`NEWSDATA_API_KEY`, prestando especial atención a: (1) `language` vs `lang`, (2) presencia de
`source_name`, (3) la forma exacta del cuerpo del error 429.
