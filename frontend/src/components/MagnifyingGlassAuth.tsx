import React from 'react';

const MagnifyingGlassAuth = () => {
  return (
    <div className="magnifier-container">
      <div className="magnifier-lens glass neon-border-orange">
        <div className="auth-form">
          <h2 className="text-neon-orange" style={{ marginBottom: '1.5rem', textAlign: 'center' }}>Login</h2>
          <form style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem' }}>
            <div className="input-group">
              <label>Email</label>
              <input type="email" placeholder="Enter your email" />
            </div>
            <div className="input-group">
              <label>Password</label>
              <input type="password" placeholder="••••••••" />
            </div>
            <button className="auth-button neon-border-orange" type="button">
              Sign In
            </button>
            <p style={{ textAlign: 'center', fontSize: '0.9rem', opacity: 0.7 }}>
              Don't have an account? <span className="text-neon-yellow" style={{ cursor: 'pointer' }}>Register</span>
            </p>
          </form>
        </div>
      </div>
      <div className="magnifier-handle neon-border-orange"></div>
    </div>
  );
};

export default MagnifyingGlassAuth;
