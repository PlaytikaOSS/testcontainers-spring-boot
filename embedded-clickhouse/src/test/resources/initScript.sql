CREATE DATABASE test;
CREATE TABLE test.users (id UInt32, first_name String, last_name String) ENGINE = Log;
INSERT INTO test.users VALUES (1, 'first_name_test', 'last_name_test');
