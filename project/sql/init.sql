CREATE DATABASE IF NOT EXISTS housing_db DEFAULT CHARSET utf8mb4;
USE housing_db;

DROP TABLE IF EXISTS house_listings;
CREATE TABLE house_listings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    district VARCHAR(20) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    area DECIMAL(8,2) NOT NULL,
    floor INT NOT NULL,
    house_age DECIMAL(6,2) NOT NULL,
    distance_to_subway DECIMAL(6,2) NOT NULL,
    listing_type VARCHAR(10) NOT NULL,
    INDEX idx_qry (district, year, month, listing_type)
);