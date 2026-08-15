-- Catalogo inicial de departamentos y categorias, clasificado segun lo definido
-- por la Municipalidad. "Transversal" es un departamento especial para las
-- denuncias de irregularidades/corrupcion, que pueden involucrar a cualquier oficina.

INSERT INTO departamento (nombre) VALUES
    ('Servicios Públicos'),
    ('Obras e Infraestructura'),
    ('Administración Financiera - DAFIM'),
    ('Asuntos Municipales'),
    ('Transversal / Alcaldía');

-- Servicios Públicos
INSERT INTO categoria (nombre, id_departamento, tipos_permitidos) VALUES
    ('Agua / Tuberías', (SELECT id FROM departamento WHERE nombre = 'Servicios Públicos'), NULL),
    ('Luz / Alumbrado / Cable eléctrico / Poste dañado', (SELECT id FROM departamento WHERE nombre = 'Servicios Públicos'), NULL),
    ('Drenajes / Alcantarillado tapado / Fuga de aguas negras / Contaminación', (SELECT id FROM departamento WHERE nombre = 'Servicios Públicos'), NULL),
    ('Recolección de basura / Limpieza de áreas públicas', (SELECT id FROM departamento WHERE nombre = 'Servicios Públicos'), NULL);

-- Obras e Infraestructura
INSERT INTO categoria (nombre, id_departamento, tipos_permitidos) VALUES
    ('Calles / Banquetas / Obstrucción de vía pública', (SELECT id FROM departamento WHERE nombre = 'Obras e Infraestructura'), NULL),
    ('Baches', (SELECT id FROM departamento WHERE nombre = 'Obras e Infraestructura'), NULL),
    ('Puentes', (SELECT id FROM departamento WHERE nombre = 'Obras e Infraestructura'), NULL),
    ('Semáforos', (SELECT id FROM departamento WHERE nombre = 'Obras e Infraestructura'), NULL),
    ('Señalización', (SELECT id FROM departamento WHERE nombre = 'Obras e Infraestructura'), NULL),
    ('Derrumbes / Árboles caídos', (SELECT id FROM departamento WHERE nombre = 'Obras e Infraestructura'), NULL);

-- Administración Financiera - DAFIM
INSERT INTO categoria (nombre, id_departamento, tipos_permitidos) VALUES
    ('IUSI', (SELECT id FROM departamento WHERE nombre = 'Administración Financiera - DAFIM'), NULL),
    ('Arbitrios o impuestos', (SELECT id FROM departamento WHERE nombre = 'Administración Financiera - DAFIM'), NULL),
    ('Trámites', (SELECT id FROM departamento WHERE nombre = 'Administración Financiera - DAFIM'), NULL),
    ('Mercado', (SELECT id FROM departamento WHERE nombre = 'Administración Financiera - DAFIM'), NULL),
    ('Cementerio', (SELECT id FROM departamento WHERE nombre = 'Administración Financiera - DAFIM'), NULL);

-- Asuntos Municipales
INSERT INTO categoria (nombre, id_departamento, tipos_permitidos) VALUES
    ('Mal trato / Demoras en la atención', (SELECT id FROM departamento WHERE nombre = 'Asuntos Municipales'), 'QUEJA'),
    ('Programas sociales', (SELECT id FROM departamento WHERE nombre = 'Asuntos Municipales'), NULL),
    ('Sugerencias generales', (SELECT id FROM departamento WHERE nombre = 'Asuntos Municipales'), 'SUGERENCIA');

-- Transversal (cualquier oficina): denuncias de irregularidades o corrupción
INSERT INTO categoria (nombre, id_departamento, tipos_permitidos) VALUES
    ('Denuncia de irregularidades / corrupción', (SELECT id FROM departamento WHERE nombre = 'Transversal / Alcaldía'), 'DENUNCIA');

-- Usuario administrador inicial (sin contraseña cifrada, segun lo definido para el proyecto)
INSERT INTO usuario (nombre, username, password, rol, estado) VALUES
    ('Administrador del Sistema', 'admin', 'admin123', 'ADMINISTRADOR', 'ACTIVO');
