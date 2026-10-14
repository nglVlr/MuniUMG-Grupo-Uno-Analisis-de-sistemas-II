-- Segundo Auxiliar, Supervisor y Jefe de Departamento de prueba.
-- El CU-10 FA08 exige reasignar los casos pendientes antes de inactivar al usuario,
-- y el destino debe ser otro usuario activo del mismo rol, por eso cada uno de estos
-- roles necesita al menos dos usuarios para poder probar la reasignación.
-- Las credenciales quedan documentadas en CREDENCIALES_PRUEBA.md

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190131', 'Auxiliar de Prueba 2', 'auxiliar2.prueba@miumg.edu.gt',
       '$2a$10$JLoD5cSdM3Xx8pK0c.YCPuorI0fSyiSf1A0XpB8xlvDEtBv6Aj2UK',
       (SELECT id FROM rol WHERE rol = 'Auxiliar'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'auxiliar2.prueba@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190132', 'Supervisor de Prueba 2', 'supervisor2.prueba@miumg.edu.gt',
       '$2a$10$sI7OPP0NclGdmXTdyr0NXeejP4e1GDDue8Mu6DW9wiz4gLYxIxAny',
       (SELECT id FROM rol WHERE rol = 'Supervisor'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'supervisor2.prueba@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190133', 'Jefatura de Prueba 2', 'jefatura2.prueba@miumg.edu.gt',
       '$2a$10$au7Ub5pwnOhi9iyZdMKooOWztto98JXwTn53vULilUimcY18pi6GO',
       (SELECT id FROM rol WHERE rol = 'Jefe'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'jefatura2.prueba@miumg.edu.gt');
