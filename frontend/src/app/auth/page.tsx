import MagnifyingGlassAuth from '@/components/MagnifyingGlassAuth';
import Link from 'next/link';

export default function AuthPage() {
  return (
    <div style={{ 
      minHeight: '100vh', 
      display: 'flex', 
      flexDirection: 'column',
      alignItems: 'center', 
      justifyContent: 'center',
      padding: '2rem'
    }}>
      <Link href="/" style={{ 
        position: 'absolute', 
        top: '2rem', 
        left: '2rem', 
        display: 'flex', 
        alignItems: 'center', 
        gap: '0.5rem',
        opacity: 0.8
      }}>
        ← Back to Home
      </Link>
      
      <MagnifyingGlassAuth />
      
      <div style={{ 
        marginTop: '2rem', 
        textAlign: 'center', 
        maxWidth: '500px',
        opacity: 0.8
      }}>
        <p>
          Access the <span className="text-neon-orange">TruthLens</span> system. 
          The magnifying glass symbolizes our mission of thorough fact-checking. Log in to manage your verifications.
        </p>
      </div>
    </div>
  );
}
