// Load environment variables from .env file
require('dotenv').config();

// Set default environment variables if not provided
process.env.DB_HOST = process.env.DB_HOST || 'localhost';
process.env.DB_PORT = process.env.DB_PORT || '5432';
process.env.DB_NAME = process.env.DB_NAME || 'hdim';
process.env.DB_USER = process.env.DB_USER || 'hdim_user';
process.env.DB_PASSWORD = process.env.DB_PASSWORD || '';
process.env.HDIM_API_URL = process.env.HDIM_API_URL || 'http://localhost:3000';

// Global test timeout
jest.setTimeout(30000);
