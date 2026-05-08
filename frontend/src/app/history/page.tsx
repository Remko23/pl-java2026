import React from 'react';

export default function HistoryPage() {
  return (
    <div style={{ 
      minHeight: '80vh', 
      display: 'flex', 
      flexDirection: 'column',
      alignItems: 'center', 
      justifyContent: 'center',
      padding: '2rem',
      textAlign: 'center'
    }}>
      <h1 style={{ 
        fontSize: '8rem', 
        fontWeight: 800, 
        letterSpacing: '0.1em',
        background: 'linear-gradient(135deg, #FF6B6B 0%, #FF8E53 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        marginBottom: '2rem',
        textShadow: '0 0 40px rgba(255, 107, 107, 0.3)'
      }} className="neon-text-orange">
        SOON
      </h1>
      
      <div style={{ 
        fontSize: '1.5rem', 
        fontStyle: 'italic', 
        opacity: 0.8,
        maxWidth: '600px',
        lineHeight: 1.6,
        padding: '2rem',
        borderTop: '1px solid rgba(255, 255, 255, 0.1)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)'
      }}>
        <p style={{ marginBottom: '1rem' }}>
          "Good things come to those who wait."
        </p>
        <p style={{ fontSize: '1.1rem', opacity: 0.7, fontStyle: 'normal' }}>
          ~ Sensei Wu
        </p>
      </div>
    </div>
  );
}
