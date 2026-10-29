# api-incident

API para la gestión de incidentes de la Mesa Operativa Campus UTP.

Por defecto la API se ejecuta en `http://localhost:8081`; el puerto se puede
cambiar con la variable de entorno `PORT`.

## Documentación

Swagger UI está disponible directamente en `/` y `/docs`, sin redirigir la
barra de direcciones a `/swagger-ui/index.html`.

El contrato OpenAPI en formato JSON se publica en `/openapi.json`. La ruta
antigua `/v3/api-docs` redirige a la nueva ruta.

Los endpoints están agrupados en Swagger por etiquetas y cada operación incluye
un resumen y una descripción:

| Etiqueta | Contenido |
|----------|-----------|
| Autenticación | Inicio de sesión, sesión actual y cierre de sesión |
| Usuarios | Consulta, creación, roles, contraseña y eliminación de usuarios |
| Incidentes | Gestión de incidentes y estadísticas |

### Convenciones de error

Los endpoints de usuarios e incidentes devuelven los errores de validación,
sesión y permisos con este formato:

```json
{
  "status": "error",
  "message": "Descripción del error"
}
```

## Últimos cambios en la API

- Los endpoints se agruparon y documentaron en Swagger con las etiquetas
  `Autenticación`, `Usuarios` e `Incidentes`, incluyendo resumen y descripción
  por operación.
- El contrato OpenAPI se publica en `/openapi.json`; `/v3/api-docs` se mantiene
  como redirección por compatibilidad.
- Las rutas de usuarios y roles se unificaron bajo `/api/users`; ya no existen
  rutas duplicadas bajo `/api/auth`.
- Se agregó `PATCH /api/users/{id}/password` para cambiar la contraseña, con
  validación de la contraseña actual para el propio usuario.
- Se agregó `DELETE /api/users/{id}` para eliminar usuarios, disponible solo
  para `ADMIN` y con respuesta `204 No Content`.
- El login acepta usuario o correo, las sesiones se guardan en memoria y los
  cambios de rol se aplican de inmediato sobre los tokens ya emitidos.

## Autenticación

La autenticación se realiza contra la tabla `users` de SQLite. Las contraseñas se
almacenan con hash BCrypt y la tabla se crea automáticamente al iniciar la
aplicación (`spring.jpa.hibernate.ddl-auto=update`).

### Endpoints de autenticación

| Método | Ruta | Descripción | Autorización |
|--------|------|-------------|--------------|
| POST | `/api/auth/login` | Inicia sesión con usuario o correo | Público |
| GET | `/api/auth/me` | Devuelve el usuario del token actual | Token válido |
| POST | `/api/auth/logout` | Cierra la sesión del token actual | Token válido |

Ejemplo de inicio de sesión (el campo `username` acepta usuario o correo):

```json
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

Respuesta correcta (`200 OK`):

```json
{
  "authenticated": true,
  "token": "utp-token-...",
  "username": "admin",
  "name": "Admin TI UTP",
  "email": "admin@utp.edu.pe",
  "role": "ADMIN",
  "message": "Autenticación exitosa"
}
```

Con credenciales incorrectas se devuelve `401 Unauthorized` y
`authenticated: false`. Las sesiones se mantienen en memoria, por lo que se
pierden al reiniciar la aplicación.

## Usuarios

| Método | Ruta | Descripción | Autorización |
|--------|------|-------------|--------------|
| GET | `/api/users` | Lista todos los usuarios | Solo `ADMIN` |
| POST | `/api/users` | Registra un nuevo usuario | Público, solo crea `OPERATOR`; `ADMIN` puede asignar otros roles |
| GET | `/api/users/{id}` | Obtiene un usuario por id | Solo `ADMIN` |
| PATCH | `/api/users/{id}/role` | Cambia el rol de un usuario | Solo `ADMIN` |
| PATCH | `/api/users/{id}/password` | Cambia la contraseña de un usuario | El propio usuario con su contraseña actual, o `ADMIN` para cualquiera |
| DELETE | `/api/users/{id}` | Elimina un usuario | Solo `ADMIN` |

Ejemplo de registro:

```json
POST /api/users
{
  "username": "nuevo.usuario",
  "name": "Nuevo Usuario",
  "email": "nuevo.usuario@utp.edu.pe",
  "password": "clave123",
  "role": "OPERATOR"
}
```

El registro devuelve `201 Created` con los datos públicos del usuario (nunca la
contraseña):

```json
{
  "id": 5,
  "username": "nuevo.usuario",
  "name": "Nuevo Usuario",
  "email": "nuevo.usuario@utp.edu.pe",
  "role": "OPERATOR",
  "createdAt": "2026-10-29T14:04:30"
}
```

Roles permitidos: `ADMIN`, `SUPERVISOR`, `OPERATOR`, `SUPPORT`.

Validaciones del registro:

- `username` y `password` son obligatorios; la contraseña debe tener al menos
  6 caracteres.
- El correo es opcional, pero si se envía debe tener un formato válido.
- `username` y `email` no pueden repetirse (`409 Conflict`).
- Sin token, el registro solo puede crear usuarios `OPERATOR`. Un `ADMIN` puede
  asignar cualquier rol permitido; otro rol solicitado sin ser `ADMIN` devuelve
  `403 Forbidden`.

Ejemplo para cambiar el rol de un usuario (solo `ADMIN`):

```json
PATCH /api/users/5/role
Authorization: Bearer utp-token-...

{
  "role": "SUPERVISOR"
}
```

El cambio se aplica de inmediato, incluso sobre tokens ya emitidos para ese
usuario. `PATCH /api/users/{id}/role` es la única vía para actualizar el rol; no
existen rutas duplicadas bajo `/api/auth`.

Ejemplo para cambiar la propia contraseña:

```json
PATCH /api/users/5/password
Authorization: Bearer utp-token-...

{
  "currentPassword": "clave123",
  "newPassword": "nuevaClave123"
}
```

Reglas del cambio de contraseña:

- `newPassword` es obligatorio y debe tener al menos 6 caracteres.
- El propio usuario debe enviar `currentPassword`; si es incorrecta se devuelve
  `400 Bad Request`.
- Un `ADMIN` puede omitir `currentPassword` al cambiar la contraseña de otro
  usuario.
- Un usuario distinto de `ADMIN` solo puede cambiar su propia contraseña; en
  caso contrario se devuelve `403 Forbidden`.
- Si el usuario no existe se devuelve `404 Not Found`.
- Por compatibilidad también se aceptan los alias `oldPassword` y `password`.

Para eliminar un usuario, el `ADMIN` usa `DELETE /api/users/{id}` y recibe
`204 No Content`. Si el usuario no existe se devuelve `404 Not Found`; sin token
se devuelve `401 Unauthorized` y con un rol distinto de `ADMIN`, `403 Forbidden`.

### Usuarios iniciales

| Usuario | Correo | Contraseña | Rol |
|---------|--------|------------|-----|
| `admin` | `admin@utp.edu.pe` | `admin123` | `ADMIN` |
| `supervisor` | `C32007@utp.edu.pe` | `utp2025` | `SUPERVISOR` |
| `operador` | `operador@utp.edu.pe` | `operador123` | `OPERATOR` |
| `utp` | `soporte@utp.edu.pe` | `utp2025` | `SUPPORT` |

Para consumir endpoints protegidos, envía el token devuelto por el login:

```
Authorization: Bearer utp-token-...
```

## Incidentes

| Método | Ruta | Descripción | Autorización |
|--------|------|-------------|--------------|
| GET | `/api/incidents` | Lista los incidentes | Público |
| POST | `/api/incidents` | Crea un incidente | Cualquier usuario autenticado |
| GET | `/api/incidents/{id}` | Obtiene un incidente | Público |
| PATCH | `/api/incidents/{id}/status` | Actualiza el estado | Solo `ADMIN` |
| PATCH | `/api/incidents/{id}/priority` | Actualiza la prioridad | Solo `ADMIN` |
| DELETE | `/api/incidents/{id}` | Elimina un incidente | Público |

Para publicar un incidente o actualizar su estado debes enviar el token del login:

```
Authorization: Bearer utp-token-...
```

- Sin token: `401 Unauthorized`.
- Token válido pero rol distinto de `ADMIN` al cambiar el estado o la prioridad: `403 Forbidden`.
- Si falta el campo `status` o `priority` al actualizar: `400 Bad Request`.

Ejemplo para crear un incidente:

```json
POST /api/incidents
Authorization: Bearer utp-token-...

{
  "title": "Sin conexión en el Pabellón A",
  "description": "Los equipos del laboratorio no tienen red.",
  "severity": "High",
  "category": "Redes",
  "location": "Pabellón A",
  "message": "Reportado por el docente de turno"
}
```

Campos y valores por defecto:

| Campo | Obligatorio | Valor por defecto |
|-------|-------------|-------------------|
| `title` | No | Cadena vacía |
| `description` | No | Cadena vacía |
| `severity` | No | `Low` (también se acepta el alias `priority`) |
| `category` | No | `General` |
| `location` | No | `Campus UTP` |
| `message` | No | Cadena vacía (también se acepta el alias `rejectionReason`) |
| `status` | No | `Active`; solo un `ADMIN` puede definir otro estado al crear |

La creación devuelve `201 Created` con el incidente almacenado, incluyendo `id`,
`ticketCode` (por ejemplo `INC-1005`), `createdAt` y `updatedAt`.

Para actualizar el estado:

```json
PATCH /api/incidents/5/status
Authorization: Bearer utp-token-...

{
  "status": "CLOSED",
  "message": "Incidente resuelto"
}
```

El campo `message` es opcional. Al pasar a `CLOSED` o `Inactive` se guarda el
mensaje indicado; en otros estados el mensaje se limpia.

Para actualizar la prioridad:

```json
PATCH /api/incidents/5/priority
Authorization: Bearer utp-token-...

{
  "priority": "High"
}
```

Valores admitidos: `High`, `Medium` y `Low`. La prioridad por defecto al crear
un incidente es `Low`.

### Estadísticas

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/incidents/stats` | Estadísticas generales |
| GET | `/api/incidents/stats/count` | Total de incidentes |
| GET | `/api/incidents/stats/total` | Total de incidentes |
| GET | `/api/incidents/stats/by-category` | Conteo por categoría |
| GET | `/api/incidents/stats/by-severity` | Conteo por severidad |
| GET | `/api/incidents/stats/by-status` | Conteo por estado |

Respuesta de `GET /api/incidents/stats`:

```json
{
  "total": 12,
  "active": 8,
  "inactive": 4,
  "byCategory": {
    "Redes": 5,
    "Hardware": 4,
    "General": 3
  },
  "bySeverity": {
    "High": 4,
    "Medium": 6,
    "Low": 2
  },
  "byStatus": {
    "Active": 6,
    "IN_PROGRESS": 2,
    "CLOSED": 4
  }
}
```

`active` agrupa los estados `Active`, `OPEN` e `IN_PROGRESS`; `inactive` agrupa
`Inactive`, `CLOSED` y `RESOLVED`. Los conteos por categoría, severidad y estado
son mapas de `valor → cantidad`.
