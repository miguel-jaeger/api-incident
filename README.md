# api-incident

API para la gestión de incidentes de la Mesa Operativa Campus UTP.

## Autenticación

La autenticación se realiza contra la tabla `users` de SQLite. Las contraseñas se
almacenan con hash BCrypt y la tabla se crea automáticamente al iniciar la
aplicación (`spring.jpa.hibernate.ddl-auto=update`).

### Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Inicia sesión con usuario o correo |
| POST | `/api/auth/users` | Registra un nuevo usuario |
| GET | `/api/auth/me` | Devuelve el usuario del token actual |
| POST | `/api/auth/logout` | Cierra la sesión del token actual |

Ejemplo de registro:

```json
POST /api/auth/users
{
  "username": "nuevo.usuario",
  "name": "Nuevo Usuario",
  "email": "nuevo.usuario@utp.edu.pe",
  "password": "clave123",
  "role": "OPERATOR"
}
```

Roles permitidos: `ADMIN`, `SUPERVISOR`, `OPERATOR`, `SUPPORT`.

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

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/incidents` | Lista los incidentes |
| POST | `/api/incidents` | Crea un incidente |
| GET | `/api/incidents/{id}` | Obtiene un incidente |
| PATCH | `/api/incidents/{id}/status` | Actualiza el estado |
| DELETE | `/api/incidents/{id}` | Elimina un incidente |
| GET | `/api/incidents/stats` | Estadísticas generales |
| GET | `/api/incidents/stats/count` | Total de incidentes |
| GET | `/api/incidents/stats/total` | Total de incidentes |
| GET | `/api/incidents/stats/by-category` | Conteo por categoría |
| GET | `/api/incidents/stats/by-severity` | Conteo por severidad |
| GET | `/api/incidents/stats/by-status` | Conteo por estado |
