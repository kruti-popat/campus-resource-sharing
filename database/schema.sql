-- Campus Resource Sharing Platform Database Schema
-- Create database
CREATE DATABASE IF NOT EXISTS campus_resource_db;
USE campus_resource_db;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Resources Table
CREATE TABLE IF NOT EXISTS resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    condition_status VARCHAR(50) NOT NULL,
    owner_id BIGINT NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    transaction_type VARCHAR(20) NOT NULL,
    price DECIMAL(10, 2),
    rental_duration VARCHAR(50),
    exchange_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_owner (owner_id),
    INDEX idx_category (category),
    INDEX idx_available (is_available),
    INDEX idx_transaction_type (transaction_type)
);

-- Requests Table
CREATE TABLE IF NOT EXISTS requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    message TEXT,
    owner_verified BOOLEAN DEFAULT FALSE,
    requester_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_resource (resource_id),
    INDEX idx_requester (requester_id),
    INDEX idx_owner (owner_id),
    INDEX idx_status (status)
);

-- Messages Table (for chat)
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES requests(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_request (request_id),
    INDEX idx_sender (sender_id),
    INDEX idx_created_at (created_at)
);

-- Sample Data (Optional - for testing)
INSERT INTO users (username, email, password, full_name, phone) VALUES
('john_doe', 'john@example.com', 'password123', 'John Doe', '1234567890'),
('jane_smith', 'jane@example.com', 'password123', 'Jane Smith', '0987654321');

INSERT INTO resources (title, description, category, condition_status, owner_id, transaction_type) VALUES
('Introduction to Java Programming', 'Complete guide to Java programming language', 'Books', 'Good', 1, 'SHARE'),
('MacBook Pro 2020', '13-inch MacBook Pro in excellent condition', 'Electronics', 'Excellent', 1, 'SHARE'),
('Calculus Textbook', 'Calculus textbook for engineering students', 'Books', 'Fair', 2, 'SHARE');
