-- 15 Técnicos de prueba (3 por cada uno de los 5 departamentos), para probar
-- el algoritmo de asignación del CU-04 (menor carga de casos activos).
-- Contraseña para todos: Tecnico123

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190113', 'Mario Castillo', 'mcastillo1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'mcastillo1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190114', 'Ana Ramirez', 'aramirez1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'aramirez1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190115', 'Carlos Estrada', 'cestrada1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'cestrada1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190116', 'Jose Morales', 'jmorales1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Servicios Publicos'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'jmorales1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190117', 'Brenda Sical', 'bsical1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Servicios Publicos'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'bsical1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190118', 'Luis Choc', 'lchoc1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Servicios Publicos'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'lchoc1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190119', 'Karla Xitumul', 'kxitumul1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Asuntos Municipales'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'kxitumul1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190120', 'Diego Paz', 'dpaz1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Asuntos Municipales'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'dpaz1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190121', 'Melissa Roquel', 'mroquel1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Asuntos Municipales'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'mroquel1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190122', 'Fernando Boc', 'fboc1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Recursos Humanos'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'fboc1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190123', 'Priscila Tzoc', 'ptzoc1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Recursos Humanos'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'ptzoc1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190124', 'Edwin Say', 'esay1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Recursos Humanos'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'esay1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190125', 'Gabriela Us', 'gus1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Tesoreria'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'gus1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190126', 'Rolando Ixchop', 'rixchop1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Tesoreria'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'rixchop1@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190127', 'Cristina Cux', 'ccux1@miumg.edu.gt',
       '$2a$10$HP7DgxCMH.4bTMI6lbEAQeZi4mzxExlKileXG/kc/r3OquDsMmRUS',
       (SELECT id FROM rol WHERE rol = 'Tecnico'),
       (SELECT id FROM departamento WHERE nombre = 'Tesoreria'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'ccux1@miumg.edu.gt');
