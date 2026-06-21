db = db.getSiblingDB('truthlens_history');

db.createUser({
  user: 'truthlens_app',
  pwd: 'truthlens_mongo_pass',
  roles: [
    { role: 'readWrite', db: 'truthlens_history' }
  ]
});

db.createCollection('verification_history');
db.verification_history.createIndex({ userId: 1, createdAt: -1 });
