"use client";

import React, { useState, useEffect, useRef } from 'react';

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

interface AnalyzeClientProps {
  token: string;
}

export default function AnalyzeClient({ token }: AnalyzeClientProps) {
  const [activeTab, setActiveTab] = useState<'screenshot' | 'text'>('screenshot');
  const [textInput, setTextInput] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [verificationId, setVerificationId] = useState<string | null>(null);
  const [status, setStatus] = useState<VerificationStatus>('IDLE');
  const [progress, setProgress] = useState(0);
  const [message, setMessage] = useState('');
  const [result, setResult] = useState<JuryReport | null>(null);
  const [isReasoningExpanded, setIsReasoningExpanded] = useState(false);
  const [lastAnalyzedContent, setLastAnalyzedContent] = useState<string | null>(null);

  const [isSystemReady, setIsSystemReady] = useState<boolean>(false);

  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const handleAnalyze = async () => {
    if (activeTab === 'text' && !textInput.trim()) return;
    if (activeTab === 'screenshot' && !selectedFile) return;

    setStatus('UPLOADING');
    setProgress(0);
    setMessage('Sending request to server...');
    setResult(null);
    setLastAnalyzedContent(activeTab === 'text' ? textInput : selectedFile!.name);

    try {
      let response;
      if (activeTab === 'text') {
        const headers: Record<string, string> = {
          'Content-Type': 'application/json'
        };
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        response = await fetch('http://127.0.0.1:8080/api/v1/verifications', {
          method: 'POST',
          headers,
          body: JSON.stringify({ claimText: textInput })
        });
      } else {
        const formData = new FormData();
        formData.append('file', selectedFile!);
        const headers: Record<string, string> = {};
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        response = await fetch('http://127.0.0.1:8080/api/v1/verifications', {
          method: 'POST',
          headers,
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
        const headers: Record<string, string> = {};
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        const res = await fetch(`http://127.0.0.1:8080/api/v1/verifications/${verificationId}`, {
          headers
        });
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
  }, [verificationId, status, token]);

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const res = await fetch('http://127.0.0.1:8080/api/v1/health');
        if (res.ok) {
          const data = await res.json();
          setIsSystemReady(data.status === 'UP');
        } else {
          setIsSystemReady(false);
        }
      } catch (e) {
        setIsSystemReady(false);
      }
    };

    checkHealth();
    const interval = setInterval(checkHealth, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const isAnalyzing = status !== 'IDLE' && status !== 'COMPLETED' && status !== 'FAILED';

  const currentContent = activeTab === 'text' ? textInput : selectedFile?.name || null;
  const hasInput = currentContent !== null && currentContent.trim() !== '';
  const isInputChanged = lastAnalyzedContent !== currentContent;
  const isReadyToAnalyze = hasInput && isInputChanged && !isAnalyzing && isSystemReady;

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

        {status === 'COMPLETED' && result && (
          <div style={{ marginBottom: '3rem', padding: '2rem', borderRadius: '12px', background: 'rgba(0,0,0,0.3)', border: result.finalVerdict === 'TRUE' ? '1px solid #00ff00' : '1px solid #ff0000' }}>
            <h2 style={{ textAlign: 'center', fontSize: '2.5rem', color: result.finalVerdict === 'TRUE' ? '#00ff00' : '#ff0000', marginBottom: '1rem' }}>
              {result.finalVerdict === 'TRUE' ? 'TRUE / FACT' : 'FALSE / FAKE NEWS'}
            </h2>
            <div style={{ textAlign: 'center', marginBottom: '2rem', fontSize: '1.2rem' }}>
              Confidence Score: <strong>{result.averageConfidence.toFixed(1)}%</strong>
            </div>

            <div style={{ textAlign: 'center' }}>
              <button
                onClick={() => setIsReasoningExpanded(!isReasoningExpanded)}
                style={{ padding: '0.5rem 1rem', background: 'transparent', border: '1px solid #b026ff', color: '#b026ff', borderRadius: '8px', cursor: 'pointer' }}
              >
                {isReasoningExpanded ? 'Hide AI Jury Reasoning ▲' : 'Show AI Jury Reasoning ▼'}
              </button>
            </div>

            {isReasoningExpanded && (
              <div style={{ marginTop: '1.5rem', whiteSpace: 'pre-wrap', textAlign: 'left', lineHeight: 1.6, opacity: 0.9, padding: '1.5rem', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                {result.aggregatedReasoning.split('\n').map((line, idx) => {
                  const colonIdx = line.indexOf(':');
                  if (colonIdx !== -1) {
                    return (
                      <div key={idx} style={{ marginBottom: '1rem' }}>
                        <strong style={{ color: '#b026ff' }}>AI {idx + 1}:</strong> {line.substring(colonIdx + 1).trim()}
                      </div>
                    );
                  }
                  return <div key={idx} style={{ marginBottom: '1rem' }}>{line}</div>;
                })}
              </div>
            )}
          </div>
        )}

        {status === 'FAILED' && (
          <div style={{ marginBottom: '3rem', padding: '2rem', textAlign: 'center', color: '#ff4d4d', background: 'rgba(255,0,0,0.1)', border: '1px solid red', borderRadius: '12px' }}>
            <h3 style={{ fontSize: '1.5rem', marginBottom: '1rem' }}>Verification Failed</h3>
            <p>{message}</p>
          </div>
        )}

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

        <div style={{ marginTop: '2rem' }}>
          {isAnalyzing ? (
            <div style={{ padding: '4rem 2rem', textAlign: 'center', background: 'rgba(255,255,255,0.02)', borderRadius: '16px', border: '1px solid rgba(255,255,255,0.05)' }}>
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
              <p style={{ marginTop: '1rem', fontWeight: 'bold', color: '#b026ff' }}>{progress}%</p>
            </div>
          ) : activeTab === 'screenshot' ? (
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

        <div style={{ marginTop: '2.5rem', textAlign: 'center' }}>
          <button
            onClick={handleAnalyze}
            disabled={isAnalyzing || !isSystemReady}
            className={`glass ${isReadyToAnalyze ? 'glow-active text-neon-purple' : 'neon-border-purple text-neon-purple'}`}
            style={{
              padding: '1rem 3rem',
              fontSize: '1.1rem',
              fontWeight: 600,
              cursor: (isAnalyzing || !isSystemReady) ? 'not-allowed' : 'pointer',
              background: isAnalyzing ? 'rgba(176, 38, 255, 0.2)' : 'transparent',
              opacity: (isAnalyzing || !isSystemReady) ? 0.5 : 1,
              transition: 'all 0.3s'
            }}
          >
            {isAnalyzing ? 'Analyzing...' : (!isSystemReady ? 'System Booting...' : 'Analyze Now')}
          </button>
        </div>

      </div>

    </div>
  );
}
