package com.truthlens.backend.config;

// TODO: Implement Redis connection and RedisTemplate configuration.
// Responsibilities:
//   - Configure RedisConnectionFactory (Lettuce)
//   - Configure RedisTemplate<String, Object> with JSON serialization
//   - Set default TTL for verification state keys (e.g. 15 minutes)
// Redis is used ONLY as ephemeral state store — no persistence requirements.

public class RedisConfig {
}
