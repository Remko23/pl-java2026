"use client";

import React, { useState } from 'react';
import Link from 'next/link';

export default function AnalyzePage() {
  const [activeTab, setActiveTab] = useState<'screenshot' | 'text'>('screenshot');

  return (
    <div style={{ 
      minHeight: '100vh', 
      padding: '2rem',
      maxWidth: '800px',
      margin: '0 auto',
      display: 'flex',
      flexDirection: 'column'
    }}>

      <header style={{ textAlign: 'center', marginBottom: '3rem' }}>
        <h1 style={{ fontSize: '3rem', marginBottom: '1rem' }}>
          TruthLens <span className="text-neon-purple">Analysis</span>
        </h1>
        <p style={{ opacity: 0.8, fontSize: '1.1rem' }}>
          Provide information to verify.
        </p>
      </header>

      <div className="glass" style={{ padding: '2rem' }}>
        <div className="tab-container">
          <button 
            className={`tab-button ${activeTab === 'screenshot' ? 'active' : ''}`}
            onClick={() => setActiveTab('screenshot')}
          >
            Screenshot
          </button>
          <button 
            className={`tab-button ${activeTab === 'text' ? 'active' : ''}`}
            onClick={() => setActiveTab('text')}
          >
            Text Input
          </button>
        </div>

        <div style={{ marginTop: '2rem' }}>
          {activeTab === 'screenshot' ? (
            <div className="dropzone">
              <div style={{ fontSize: '4rem', opacity: 0.7 }}>📸</div>
              <h3 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Upload a Screenshot</h3>
              <p style={{ opacity: 0.7 }}>
                Drag and drop your screenshot here, or click to browse files.
              </p>
            </div>
          ) : (
            <div>
              <textarea 
                className="analyze-textarea"
                placeholder="Paste the content of a post, article, or message to verify..."
              ></textarea>
            </div>
          )}
        </div>

        <div style={{ marginTop: '2rem', textAlign: 'center' }}>
          <button 
            className="glass neon-border-purple text-neon-purple" 
            style={{ 
              padding: '1rem 3rem', 
              fontSize: '1.1rem', 
              fontWeight: 600, 
              cursor: 'pointer',
              background: 'transparent'
            }}
          >
            Analyze Now
          </button>
        </div>
      </div>
    </div>
  );
}
