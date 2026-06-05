// Initialize the truthlens_history database with a dedicated application user.
// This script runs automatically on first MongoDB container startup.

db = db.getSiblingDB('truthlens_history');

db.createUser({
  user: 'truthlens_app',
  pwd: 'truthlens_mongo_pass',
  roles: [
    { role: 'readWrite', db: 'truthlens_history' }
  ]
});

// Create index on userId + createdAt for efficient history queries
db.createCollection('verification_history');
db.verification_history.createIndex({ userId: 1, createdAt: -1 });
