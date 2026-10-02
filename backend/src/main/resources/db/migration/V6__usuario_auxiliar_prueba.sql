-- Usuario Auxiliar de prueba, para probar el CU-04 Bandeja Auxiliar
INSERT INTO usuario (cui, nombre, correo, password, id_rol, id_departamento, id_estado, usuario_creacion)
SELECT '1971216190112', 'Auxiliar de Prueba', 'auxiliar.prueba@miumg.edu.gt',
       '$2a$10$MQVmiKKj7SllG0dbmMg27eS8l34FHK04raay2/VqYhYXKa6WWd3dW',
       (SELECT id FROM rol WHERE rol = 'Auxiliar'),
       (SELECT id FROM departamento WHERE nombre = 'Tecnologia'),
       (SELECT id FROM estado_usuarios WHERE estado = 'Activo'),
       'system'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'auxiliar.prueba@miumg.edu.gt');
