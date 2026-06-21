"use client";

import React, { useState, useEffect } from 'react';
import Link from 'next/link';

interface VerificationHistory {
  id: string;
  userId: string;
  inputType: 'TEXT' | 'IMAGE';
  inputText: string;
  fileName?: string;
  finalVerdict: 'TRUE' | 'FALSE';
  averageConfidence: number;
  aggregatedReasoning: string;
  createdAt: string;
}

interface PageResponse {
  content: VerificationHistory[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
}

interface HistoryClientProps {
  token: string;
}

export default function HistoryClient({ token }: HistoryClientProps) {
  const [historyPage, setHistoryPage] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [sortDirection, setSortDirection] = useState<'desc' | 'asc'>('desc');
  const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});

  const fetchHistory = async (page: number, direction: 'desc' | 'asc') => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`http://127.0.0.1:8080/api/v1/history?page=${page}&size=10&sort=createdAt,${direction}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (res.ok) {
        const data: PageResponse = await res.json();
        setHistoryPage(data);
      } else {
        setError('Failed to fetch verification history from server.');
      }
    } catch (err) {
      console.error(err);
      setError('Could not connect to Gateway service.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory(currentPage, sortDirection);
  }, [currentPage, sortDirection, token]);

  const toggleExpand = (id: string) => {
    setExpandedItems(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const handleSortToggle = () => {
    const nextDirection = sortDirection === 'desc' ? 'asc' : 'desc';
    setSortDirection(nextDirection);
    setCurrentPage(0);
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      padding: '2rem',
      maxWidth: '900px',
      margin: '0 auto',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <header style={{ textAlign: 'center', marginBottom: '2.5rem' }}>
        <h1 style={{ fontSize: '3rem', marginBottom: '0.8rem' }}>
          Verification <span className="text-neon-orange">History</span>
        </h1>
        <p style={{ opacity: 0.8, fontSize: '1.1rem' }}>
          Browse your past analyzed contents and AI Jury reports.
        </p>
      </header>

      
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '1.5rem',
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        <Link href="/dashboard" className="glass" style={{
          padding: '0.6rem 1.2rem',
          fontSize: '0.95rem',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          color: 'white',
          borderRadius: '8px',
          cursor: 'pointer',
          transition: 'all 0.2s',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem'
        }}>
          <span>←</span> Back to Dashboard
        </Link>

        {historyPage && historyPage.totalElements > 0 && (
          <button
            onClick={handleSortToggle}
            className="glass"
            style={{
              padding: '0.6rem 1.2rem',
              fontSize: '0.95rem',
              border: '1px solid rgba(255, 103, 0, 0.3)',
              color: 'white',
              borderRadius: '8px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              boxShadow: '0 0 10px rgba(255, 103, 0, 0.1)'
            }}
          >
            Sort by Date: <strong className="text-neon-orange">{sortDirection === 'desc' ? 'Newest First' : 'Oldest First'}</strong>
          </button>
        )}
      </div>

      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {[1, 2, 3].map(i => (
            <div key={i} className="glass" style={{ padding: '2rem', height: '150px', opacity: 0.4, animation: 'pulse 1.5s infinite ease-in-out' }}>
              <div style={{ height: '20px', background: 'rgba(255,255,255,0.1)', width: '30%', borderRadius: '4px', marginBottom: '1rem' }}></div>
              <div style={{ height: '40px', background: 'rgba(255,255,255,0.1)', width: '80%', borderRadius: '4px' }}></div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="glass" style={{
          padding: '2.5rem',
          textAlign: 'center',
          border: '1px solid #ff4d4d',
          background: 'rgba(255, 0, 0, 0.05)',
          boxShadow: '0 0 15px rgba(255, 0, 0, 0.1)'
        }}>
          <h3 style={{ color: '#ff4d4d', fontSize: '1.4rem', marginBottom: '0.8rem' }}>Failed to Load History</h3>
          <p style={{ opacity: 0.8, marginBottom: '1.5rem' }}>{error}</p>
          <button
            onClick={() => fetchHistory(currentPage, sortDirection)}
            className="glass neon-border-orange text-neon-orange"
            style={{ padding: '0.6rem 1.5rem', cursor: 'pointer', fontWeight: 600 }}
          >
            Retry
          </button>
        </div>
      ) : !historyPage || historyPage.content.length === 0 ? (
        <div className="glass" style={{
          padding: '4rem 2rem',
          textAlign: 'center',
          border: '1px solid rgba(255,255,255,0.05)'
        }}>
          <div style={{ fontSize: '4rem', marginBottom: '1rem', opacity: 0.5 }}>📚</div>
          <h3 style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>No History Found</h3>
          <p style={{ opacity: 0.7, marginBottom: '2rem' }}>You haven't run any content verifications yet.</p>
          <Link href="/analyze" className="glass glow-active text-neon-purple" style={{
            padding: '0.8rem 2rem',
            fontWeight: 600,
            fontSize: '1.05rem',
            border: '1px solid rgba(188, 19, 254, 0.5)'
          }}>
            Analyze Your First Content
          </Link>
        </div>
      ) : (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {historyPage.content.map((item) => {
              const isExpanded = !!expandedItems[item.id];
              const isImage = item.inputType === 'IMAGE';

              return (
                <div key={item.id} className="glass" style={{
                  padding: '1.8rem',
                  border: item.averageConfidence < 50
                    ? '1px solid rgba(136, 136, 136, 0.3)'
                    : item.finalVerdict === 'TRUE'
                      ? '1px solid rgba(0, 255, 0, 0.15)'
                      : '1px solid rgba(255, 0, 0, 0.15)',
                  boxShadow: item.averageConfidence < 50
                    ? '0 0 20px rgba(136, 136, 136, 0.05)'
                    : item.finalVerdict === 'TRUE'
                      ? '0 0 20px rgba(0, 255, 0, 0.03)'
                      : '0 0 20px rgba(255, 0, 0, 0.03)',
                  transition: 'all 0.3s ease'
                }}>
                  
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
                    paddingBottom: '1rem',
                    marginBottom: '1rem',
                    flexWrap: 'wrap',
                    gap: '0.8rem'
                  }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                        <span style={{ fontSize: '1.2rem' }}>{isImage ? '📸' : '📝'}</span>
                        <strong style={{ fontSize: '1.05rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                          {isImage ? 'Image File' : 'Text Input'}
                        </strong>
                        {isImage && item.fileName && (
                          <span style={{
                            fontSize: '0.85rem',
                            opacity: 0.6,
                            background: 'rgba(255, 255, 255, 0.07)',
                            padding: '0.15rem 0.5rem',
                            borderRadius: '4px',
                            maxWidth: '180px',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap'
                          }} title={item.fileName}>
                            {item.fileName}
                          </span>
                        )}
                      </div>
                      <span style={{ fontSize: '0.85rem', opacity: 0.5 }}>
                        {formatDate(item.createdAt)}
                      </span>
                    </div>

                    
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1.2rem' }}>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{
                          fontWeight: 800,
                          fontSize: '1.25rem',
                          color: item.averageConfidence < 50 ? '#888888' : item.finalVerdict === 'TRUE' ? '#00ff00' : '#ff4d4d',
                          textShadow: item.averageConfidence < 50 ? '0 0 10px rgba(136, 136, 136, 0.3)' : item.finalVerdict === 'TRUE' ? '0 0 10px rgba(0, 255, 0, 0.3)' : '0 0 10px rgba(255, 77, 77, 0.3)'
                        }}>
                          {item.averageConfidence < 50 ? 'INCONCLUSIVE' : item.finalVerdict === 'TRUE' ? 'TRUE' : 'FALSE'}
                        </div>
                        <div style={{ fontSize: '0.8rem', opacity: 0.6, color: item.averageConfidence < 50 ? '#aaaaaa' : 'inherit' }}>
                          Confidence: {item.averageConfidence.toFixed(1)}%
                        </div>
                      </div>
                    </div>
                  </div>

                  
                  <div style={{ marginBottom: '1.5rem' }}>
                    <p style={{
                      fontSize: '1rem',
                      lineHeight: 1.6,
                      opacity: 0.95,
                      background: 'rgba(0,0,0,0.15)',
                      padding: '1rem',
                      borderRadius: '8px',
                      borderLeft: '3px solid rgba(255, 255, 255, 0.15)',
                      maxHeight: isExpanded ? 'none' : '120px',
                      overflow: 'hidden',
                      position: 'relative',
                      whiteSpace: 'pre-wrap'
                    }}>
                      {item.inputText}
                      {!isExpanded && item.inputText.length > 250 && (
                        <span style={{
                          position: 'absolute',
                          bottom: 0,
                          left: 0,
                          right: 0,
                          height: '40px',
                          background: 'linear-gradient(transparent, rgba(5,5,5,0.9))',
                          pointerEvents: 'none'
                        }} />
                      )}
                    </p>
                  </div>

                  
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <button
                      onClick={() => toggleExpand(item.id)}
                      className="glass"
                      style={{
                        padding: '0.5rem 1rem',
                        fontSize: '0.9rem',
                        background: 'transparent',
                        border: '1px solid rgba(255, 255, 255, 0.15)',
                        color: 'white',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.4rem'
                      }}
                    >
                      {isExpanded ? 'Hide Details ▲' : 'Show Details ▼'}
                    </button>

                    {item.inputText.length > 250 && !isExpanded && (
                      <button
                        onClick={() => toggleExpand(item.id)}
                        style={{
                          background: 'transparent',
                          border: 'none',
                          color: 'var(--neon-orange)',
                          cursor: 'pointer',
                          fontWeight: 600,
                          fontSize: '0.9rem'
                        }}
                      >
                        Read Full Input
                      </button>
                    )}
                  </div>

                  
                  {isExpanded && (
                    <div style={{
                      marginTop: '1.5rem',
                      borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                      paddingTop: '1.2rem',
                      animation: 'fadeIn 0.3s ease'
                    }}>
                      {item.averageConfidence < 50 && (
                        <div style={{ marginBottom: '1.2rem', padding: '1rem', background: 'rgba(136, 136, 136, 0.1)', borderLeft: '4px solid #888888', color: '#cccccc', fontSize: '0.95rem' }}>
                          <strong>Notice:</strong> The models voted for <strong>{item.finalVerdict === 'TRUE' ? 'TRUE' : 'FALSE'}</strong>, but they are not confident enough to make a definitive decision.
                        </div>
                      )}
                      <h4 style={{
                        fontSize: '1rem',
                        marginBottom: '0.8rem',
                        color: 'var(--neon-orange)',
                        letterSpacing: '0.5px'
                      }}>
                        AI JURY DETAILED REASONING
                      </h4>

                      {!item.aggregatedReasoning || item.aggregatedReasoning.trim() === "" ? (
                        <p style={{ fontStyle: 'italic', opacity: 0.5, fontSize: '0.9rem' }}>
                          No detailed reasoning provided by the AI Jury.
                        </p>
                      ) : (
                        <div style={{
                          whiteSpace: 'pre-wrap',
                          fontSize: '0.95rem',
                          lineHeight: 1.6,
                          opacity: 0.9,
                          padding: '1.2rem',
                          background: 'rgba(255,255,255,0.03)',
                          borderRadius: '8px',
                          border: '1px solid rgba(255,255,255,0.04)'
                        }}>
                          {item.aggregatedReasoning.split('\n').map((line, idx) => {
                            const colonIdx = line.indexOf(':');
                            if (colonIdx !== -1) {
                              const modelName = line.substring(0, colonIdx).trim();
                              const reasoning = line.substring(colonIdx + 1).trim();
                              return (
                                <div key={idx} style={{ marginBottom: '1rem' }}>
                                  <strong style={{ color: 'var(--neon-purple)' }}>{modelName}:</strong>
                                  <div style={{ marginTop: '0.2rem', paddingLeft: '0.5rem', borderLeft: '1px solid rgba(188,19,254,0.2)' }}>
                                    {reasoning}
                                  </div>
                                </div>
                              );
                            }
                            return <div key={idx} style={{ marginBottom: '0.8rem' }}>{line}</div>;
                          })}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          
          {historyPage.totalPages > 1 && (
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              gap: '1.5rem',
              marginTop: '3rem',
              marginBottom: '1rem'
            }}>
              <button
                disabled={historyPage.first}
                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                className="glass"
                style={{
                  padding: '0.6rem 1.5rem',
                  fontSize: '0.95rem',
                  cursor: historyPage.first ? 'not-allowed' : 'pointer',
                  opacity: historyPage.first ? 0.4 : 1,
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  color: 'white',
                  borderRadius: '8px'
                }}
              >
                ◀ Previous
              </button>

              <span style={{ fontSize: '0.95rem', opacity: 0.8 }}>
                Page <strong>{historyPage.number + 1}</strong> of <strong>{historyPage.totalPages}</strong>
              </span>

              <button
                disabled={historyPage.last}
                onClick={() => setCurrentPage(prev => prev + 1)}
                className="glass"
                style={{
                  padding: '0.6rem 1.5rem',
                  fontSize: '0.95rem',
                  cursor: historyPage.last ? 'not-allowed' : 'pointer',
                  opacity: historyPage.last ? 0.4 : 1,
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  color: 'white',
                  borderRadius: '8px'
                }}
              >
                Next ▶
              </button>
            </div>
          )}
        </>
      )}

      
      <style jsx global>{`
        @keyframes pulse {
          0%, 100% { opacity: 0.4; }
          50% { opacity: 0.2; }
        }
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(5px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}
