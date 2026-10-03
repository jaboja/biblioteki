-- Opcjonalne dane startowe.
-- Odkomentuj i uzupełnij własne dane logowania.
-- Przy ddl-auto=update plik jest wykonywany przy każdym starcie;
-- MERGE zapobiega duplikatom.

-- MERGE INTO library_account (id, library, username, password, enabled)
--   KEY (library, username)
--   VALUES (1, 'ZNO', 'jan.kowalski@example.com', 'haslo123', TRUE);
