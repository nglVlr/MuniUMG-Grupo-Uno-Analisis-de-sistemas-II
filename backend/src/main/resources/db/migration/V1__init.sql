-- Esquema inicial del Sistema de Quejas, Reclamos, Denuncias y Sugerencias

CREATE TABLE departamento (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    id_departamento     BIGINT NOT NULL REFERENCES departamento(id),
    tipos_permitidos    VARCHAR(60),
    activo              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ciudadano (
    id                  BIGSERIAL PRIMARY KEY,
    tipo_documento      VARCHAR(10) NOT NULL,
    numero_documento    VARCHAR(30) NOT NULL UNIQUE,
    nombre              VARCHAR(150) NOT NULL,
    correo              VARCHAR(150)
);

CREATE TABLE usuario (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    username            VARCHAR(60) NOT NULL UNIQUE,
    password            VARCHAR(100) NOT NULL,
    rol                 VARCHAR(30) NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    id_departamento     BIGINT REFERENCES departamento(id),
    intentos_fallidos   INT NOT NULL DEFAULT 0
);

CREATE TABLE caso (
    id                      BIGSERIAL PRIMARY KEY,
    codigo_seguimiento      VARCHAR(20) NOT NULL UNIQUE,
    tipo_caso               VARCHAR(15) NOT NULL,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'REGISTRADO',
    id_ciudadano            BIGINT NOT NULL REFERENCES ciudadano(id),
    id_categoria            BIGINT NOT NULL REFERENCES categoria(id),
    id_departamento         BIGINT NOT NULL REFERENCES departamento(id),
    id_empleado_asignado    BIGINT REFERENCES usuario(id),
    direccion_problema      VARCHAR(250) NOT NULL,
    descripcion             VARCHAR(2000) NOT NULL,
    fecha_registro          TIMESTAMP NOT NULL DEFAULT now(),
    motivo_rechazo          VARCHAR(40),
    detalle_rechazo         VARCHAR(500)
);

CREATE TABLE evidencia (
    id                  BIGSERIAL PRIMARY KEY,
    id_caso             BIGINT NOT NULL REFERENCES caso(id),
    nombre_archivo      VARCHAR(200) NOT NULL,
    tipo_archivo        VARCHAR(50) NOT NULL,
    tamano_bytes        BIGINT NOT NULL,
    ruta                VARCHAR(500) NOT NULL,
    fecha_carga         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE documento (
    id                  BIGSERIAL PRIMARY KEY,
    id_caso             BIGINT NOT NULL REFERENCES caso(id),
    tipo                VARCHAR(30) NOT NULL,
    numero_correlativo  VARCHAR(30) NOT NULL,
    id_usuario_autor    BIGINT NOT NULL REFERENCES usuario(id),
    contenido           VARCHAR(4000) NOT NULL,
    fecha_hora          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE bitacora (
    id                  BIGSERIAL PRIMARY KEY,
    accion              VARCHAR(200) NOT NULL,
    fecha_hora          TIMESTAMP NOT NULL DEFAULT now(),
    ip                  VARCHAR(50) NOT NULL,
    id_usuario          BIGINT REFERENCES usuario(id),
    id_caso             BIGINT REFERENCES caso(id)
);

CREATE TABLE notificacion (
    id                  BIGSERIAL PRIMARY KEY,
    id_caso             BIGINT NOT NULL REFERENCES caso(id),
    destinatario        VARCHAR(150) NOT NULL,
    mensaje             VARCHAR(500) NOT NULL,
    fecha_envio         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_caso_estado ON caso(estado);
CREATE INDEX idx_caso_departamento ON caso(id_departamento);
CREATE INDEX idx_bitacora_fecha ON bitacora(fecha_hora);
