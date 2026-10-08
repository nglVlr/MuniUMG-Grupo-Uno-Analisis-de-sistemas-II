-- Usuarios Supervisor y Jefe de Departamento de prueba, para probar el CU-06 y el CU-07.
-- Las credenciales quedan documentadas en CREDENCIALES_PRUEBA.md

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190128', 'Supervisor de Prueba', 'supervisor.prueba@miumg.edu.gt',
       '$2a$10$1K7FoPxAVm/4pOzUmP9r5eavARVoIkZeEfJN2IrdPSrgDzCWkeiRW',
       (SELECT id FROM rol WHERE rol = 'Supervisor'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'supervisor.prueba@miumg.edu.gt');

INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190129', 'Jefatura de Prueba', 'jefatura.prueba@miumg.edu.gt',
       '$2a$10$CjI5PR0aPgvIVv07v6YlKeIb.bB8cXDQO18VinCNnvJRPU26iRlIG',
       (SELECT id FROM rol WHERE rol = 'Jefe'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'), 'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'jefatura.prueba@miumg.edu.gt');
