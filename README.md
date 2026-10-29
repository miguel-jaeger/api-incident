# api-incident

API para la gestión de incidentes de la Mesa Operativa Campus UTP.

## Documentación

Swagger UI está disponible directamente en `/` y `/docs`, sin redirigir la
barra de direcciones a `/swagger-ui/index.html`.

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

## Usuarios

| Método | Ruta | Descripción | Autorización |
|--------|------|-------------|--------------|
| GET | `/api/users` | Lista todos los usuarios | Solo `ADMIN` |
| POST | `/api/users` | Registra un nuevo usuario | Público, solo crea `OPERATOR`; `ADMIN` puede asignar otros roles |
| GET | `/api/users/{id}` | Obtiene un usuario por id | Solo `ADMIN` |
| PATCH | `/api/users/{id}/role` | Cambia el rol de un usuario | Solo `ADMIN` |

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

Roles permitidos: `ADMIN`, `SUPERVISOR`, `OPERATOR`, `SUPPORT`.

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
| DELETE | `/api/incidents/{id}` | Elimina un incidente | Público |
Para publicar un incidente o actualizar su estado debes enviar el token del login:

```
Authorization: Bearer utp-token-...
```

- Sin token: `401 Unauthorized`.
- Token válido pero rol distinto de `ADMIN` al cambiar el estado: `403 Forbidden`.

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/incidents/stats` | Estadísticas generales |
| GET | `/api/incidents/stats/count` | Total de incidentes |
| GET | `/api/incidents/stats/total` | Total de incidentes |
| GET | `/api/incidents/stats/by-category` | Conteo por categoría |
| GET | `/api/incidents/stats/by-severity` | Conteo por severidad |
| GET | `/api/incidents/stats/by-status` | Conteo por estado |
