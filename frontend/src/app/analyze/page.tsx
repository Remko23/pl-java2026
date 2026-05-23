"use client";

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';

type VerificationStatus = 'IDLE' | 'QUEUED' | 'OCR_PROCESSING' | 'GENERATING_QUERIES' | 'SEARCHING_WEB' | 'AI_JURY_VOTING' | 'COMPLETED' | 'FAILED' | 'UPLOADING';

interface JuryReport {
  finalVerdict: string;
  averageConfidence: number;
  aggregatedReasoning: string;
}

interface VerificationResponse {
  verificationId: string;
  status: VerificationStatus;
  progressPercentage: number;
  message: string;
  result?: JuryReport;
}

export default function AnalyzePage() {
  const [activeTab, setActiveTab] = useState<'screenshot' | 'text'>('screenshot');
  const [textInput, setTextInput] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  
  const [verificationId, setVerificationId] = useState<string | null>(null);
  const [status, setStatus] = useState<VerificationStatus>('IDLE');
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState('');
  const [result, setResult] = useState<JuryReport | null>(null);
  
  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const handleAnalyze = async () => {
    if (activeTab === 'text' && !textInput.trim()) return;
    if (activeTab === 'screenshot' && !selectedFile) return;

    setStatus('UPLOADING');
    setProgress(0);
    setMessage('Sending request to server...');
    setResult(null);

    try {
      let response;
      if (activeTab === 'text') {
        response = await fetch('http://127.0.0.1:8080/api/v1/verifications', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ claimText: textInput })
        });
      } else {
        const formData = new FormData();
        formData.append('file', selectedFile!);
        response = await fetch('http://127.0.0.1:8080/api/v1/verifications', {
          method: 'POST',
          body: formData
        });
      }

      if (response.ok) {
        const data: VerificationResponse = await response.json();
        setVerificationId(data.verificationId);
        setStatus(data.status);
        setMessage(data.message || 'Queued...');
      } else {
        setStatus('FAILED');
        setMessage('Server returned an error.');
      }
    } catch (error) {
      console.error(error);
      setStatus('FAILED');
      setMessage('Failed to connect to the Gateway server on port 8080.');
    }
  };

  useEffect(() => {
    if (!verificationId) return;
    if (status === 'COMPLETED' || status === 'FAILED') return;

    const poll = async () => {
      try {
        const res = await fetch(`http://127.0.0.1:8080/api/v1/verifications/${verificationId}`);
        if (res.ok) {
          const data: VerificationResponse = await res.json();
          setStatus(data.status);
          setProgress(data.progressPercentage);
          if (data.message) setMessage(data.message);
          if (data.result) setResult(data.result);
        }
      } catch (error) {
        console.error("Polling error", error);
      }
    };

    pollingIntervalRef.current = setInterval(poll, 1500);

    return () => {
      if (pollingIntervalRef.current) clearInterval(pollingIntervalRef.current);
    };
  }, [verificationId, status]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const isAnalyzing = status !== 'IDLE' && status !== 'COMPLETED' && status !== 'FAILED';

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
        <div className="tab-container" style={{ opacity: isAnalyzing ? 0.5 : 1, pointerEvents: isAnalyzing ? 'none' : 'auto' }}>
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

        <div style={{ marginTop: '2rem', display: isAnalyzing ? 'none' : 'block' }}>
          {activeTab === 'screenshot' ? (
            <div className="dropzone" style={{ position: 'relative' }}>
              <input 
                type="file" 
                accept="image/*" 
                onChange={handleFileChange}
                style={{
                  position: 'absolute',
                  top: 0, left: 0, width: '100%', height: '100%',
                  opacity: 0, cursor: 'pointer'
                }}
              />
              <div style={{ fontSize: '4rem', opacity: 0.7 }}>📸</div>
              <h3 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Upload a Screenshot</h3>
              <p style={{ opacity: 0.7 }}>
                {selectedFile ? `Selected file: ${selectedFile.name}` : 'Drag and drop your screenshot here, or click to browse files.'}
              </p>
            </div>
          ) : (
            <div>
              <textarea 
                className="analyze-textarea"
                placeholder="Paste the content of a post, article, or message to verify..."
                value={textInput}
                onChange={(e) => setTextInput(e.target.value)}
              ></textarea>
            </div>
          )}
        </div>

        {!isAnalyzing && status !== 'COMPLETED' && (
          <div style={{ marginTop: '2rem', textAlign: 'center' }}>
            <button 
              onClick={handleAnalyze}
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
        )}

        {isAnalyzing && (
          <div style={{ marginTop: '3rem', textAlign: 'center' }}>
            <h3 style={{ fontSize: '1.5rem', marginBottom: '1rem', color: '#b026ff' }}>{status.replace(/_/g, ' ')}</h3>
            <p style={{ marginBottom: '2rem', opacity: 0.8 }}>{message}</p>
            
            <div style={{ width: '100%', height: '10px', background: 'rgba(255,255,255,0.1)', borderRadius: '5px', overflow: 'hidden' }}>
              <div style={{ 
                width: `${progress}%`, 
                height: '100%', 
                background: 'linear-gradient(90deg, #b026ff, #4d00ff)',
                transition: 'width 0.5s ease-out'
              }}></div>
            </div>
            <p style={{ marginTop: '1rem', fontWeight: 'bold' }}>{progress}%</p>
          </div>
        )}

        {status === 'FAILED' && (
          <div style={{ marginTop: '3rem', textAlign: 'center', color: '#ff4d4d' }}>
            <h3 style={{ fontSize: '1.5rem', marginBottom: '1rem' }}>Verification Failed</h3>
            <p>{message}</p>
            <button onClick={() => setStatus('IDLE')} style={{ marginTop: '1rem', padding: '0.5rem 1rem', background: 'rgba(255,0,0,0.2)', border: '1px solid red', color: 'white', borderRadius: '4px', cursor: 'pointer' }}>Try Again</button>
          </div>
        )}

        {status === 'COMPLETED' && result && (
          <div style={{ marginTop: '3rem', padding: '2rem', borderRadius: '12px', background: 'rgba(0,0,0,0.3)', border: result.finalVerdict === 'TRUE' ? '1px solid #00ff00' : '1px solid #ff0000' }}>
            <h2 style={{ textAlign: 'center', fontSize: '2.5rem', color: result.finalVerdict === 'TRUE' ? '#00ff00' : '#ff0000', marginBottom: '1rem' }}>
              {result.finalVerdict === 'TRUE' ? 'TRUE / FACT' : 'FALSE / FAKE NEWS'}
            </h2>
            <div style={{ textAlign: 'center', marginBottom: '2rem', fontSize: '1.2rem' }}>
              Confidence Score: <strong>{result.averageConfidence.toFixed(1)}%</strong>
            </div>
            
            <h4 style={{ fontSize: '1.2rem', marginBottom: '1rem', color: '#b026ff', textAlign: 'left' }}>AI Jury Reasoning:</h4>
            <div style={{ whiteSpace: 'pre-wrap', textAlign: 'left', lineHeight: 1.6, opacity: 0.9, padding: '1.5rem', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
              {result.aggregatedReasoning}
            </div>

            <div style={{ marginTop: '2rem', textAlign: 'center' }}>
               <button onClick={() => { setStatus('IDLE'); setResult(null); setTextInput(''); setSelectedFile(null); }} style={{ padding: '0.8rem 2rem', background: 'transparent', border: '1px solid #b026ff', color: '#b026ff', borderRadius: '8px', cursor: 'pointer', fontWeight: 'bold' }}>Verify Another</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
