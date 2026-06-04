CREATE DATABASE IF NOT EXISTS i_route_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'iroute'@'localhost' IDENTIFIED BY 'iroute_pw';
GRANT ALL PRIVILEGES ON i_route_db.* TO 'iroute'@'localhost';
FLUSH PRIVILEGES;
