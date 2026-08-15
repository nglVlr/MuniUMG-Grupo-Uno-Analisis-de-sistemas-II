# Sistema de Quejas, Reclamos, Denuncias y Sugerencias

Municipalidad Urbanización Monte Grande -- UMG --

## Stack

- **Backend**: Java 17 + Spring Boot 3 (Web, Data JPA, Validation, Flyway)
- **Base de datos**: PostgreSQL 16
- **Frontend**: Angular 17
- **Servidor / reverse proxy**: Nginx
- **Orquestación**: Docker Compose

## Estructura del proyecto

```
sistema-quejas/
├── docker-compose.yml       Orquesta los 4 contenedores (db, backend, frontend, nginx)
├── nginx/nginx.conf         Reverse proxy: sirve el front y redirige /api al backend
├── backend/                 API REST en Spring Boot
│   └── src/main/java/gt/muni/quejas/
│       ├── model/           Entidades JPA (Caso, Usuario, Ciudadano, Documento, etc.)
│       ├── repository/      Repositorios Spring Data
│       ├── service/         Lógica de asignación / reasignación de casos
│       ├── controller/      Endpoints REST
│       └── dto/             Objetos de transferencia de datos
├── backend/src/main/resources/db/migration/   Scripts Flyway (esquema + catálogo)
└── frontend/                Aplicación Angular (portal ciudadano + login)
```

## Cómo levantarlo

1. Copiar `.env.example` a `.env` y ajustar si es necesario.
2. Desde la raíz del proyecto:

   ```bash
   docker compose up --build
   ```

3. El sistema queda disponible en `http://localhost:8081` (a través de Nginx).
   - El backend queda accesible internamente en `http://backend:8080` (no se expone directo al host).
   - Postgres queda expuesto en el puerto `5432` del host por si se quiere conectar con un cliente de BD.

## Decisiones importantes para este scaffold

- **Sin cifrado de contraseñas**: es una decisión explícita del proyecto (no usar hash/bcrypt), tal como se indicó.
- **Código intencionalmente simple**: no hay capas extra de arquitectura (sin MapStruct, sin Specification API, sin
  seguridad con Spring Security todavía); la lógica vive directo en controladores/servicios pequeños, para que sea
  fácil de leer y extender.
- **Reasignación y disponibilidad de empleados**: el campo `estado` de `Usuario` (`ACTIVO`, `INACTIVO`, `BLOQUEADO`,
  `VACACIONES`, `PERMISO`) se usa tanto para el algoritmo de asignación (CU-05, solo toma en cuenta empleados
  `ACTIVO`) como para la reasignación manual entre actores (`PUT /api/casos/{id}/reasignar`), que valida que el
  nuevo responsable esté disponible antes de reasignar.
- **Catálogo de categorías**: cargado en `V2__seed_catalogos.sql`, clasificado en las 5 oficinas (Servicios
  Públicos, Obras e Infraestructura, DAFIM, Asuntos Municipales, y Transversal/Alcaldía para denuncias de
  irregularidades). El endpoint `GET /api/categorias?tipoCaso=QUEJA` filtra el listado según el tipo de caso
  elegido por el ciudadano.

## Pendiente / siguiente iteración

- Módulo de autenticación real (`CU-01`) conectado al backend (el login del front hoy es un placeholder).
- Endpoints de los CU-04 a CU-09 (validación, delegación, atención, supervisión, notificaciones).
- Bitácora automática (`CU-15`/`CU-16`) desde un interceptor o aspecto, en vez de llamados manuales.
- Carga real de archivos de evidencia (hoy la entidad `Evidencia` existe pero no hay endpoint de subida).
