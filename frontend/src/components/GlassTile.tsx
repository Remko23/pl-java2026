import React from 'react';
import Link from 'next/link';

interface GlassTileProps {
  title: string;
  description: string;
  icon?: string;
  color?: 'purple' | 'yellow' | 'orange';
  href?: string;
  actionText?: string;
}

const GlassTile: React.FC<GlassTileProps> = ({ title, description, icon, color = 'purple', href, actionText = 'Learn More' }) => {
  const neonClass = `neon-border-${color}`;
  const textNeonClass = `text-neon-${color}`;

  const tileContent = (
    <div className={`glass glass-tile ${neonClass}`} style={{ 
      padding: '2rem', 
      cursor: 'pointer',
      display: 'flex',
      flexDirection: 'column',
      gap: '1rem',
      height: '100%'
    }}>
      <div style={{ fontSize: '2rem' }}>{icon || '✨'}</div>
      <h3 className={textNeonClass} style={{ fontSize: '1.5rem' }}>{title}</h3>
      <p style={{ opacity: 0.8, lineHeight: 1.6 }}>{description}</p>
      
      <div style={{ marginTop: 'auto', paddingTop: '1rem' }}>
        <span style={{ 
          fontSize: '0.875rem', 
          fontWeight: 600, 
          letterSpacing: '1px', 
          textTransform: 'uppercase',
          borderBottom: '1px solid currentColor'
        }} className={textNeonClass}>
          {actionText}
        </span>
      </div>
    </div>
  );

  return href ? (
    <Link href={href} style={{ textDecoration: 'none', color: 'inherit' }}>
      {tileContent}
    </Link>
  ) : tileContent;
};

export default GlassTile;
