import { auth, signOut } from '@/auth';
import Link from 'next/link';

export default async function Header() {
  const session = await auth();

  return (
    <header style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '1.5rem 2rem',
      borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
      backgroundColor: 'rgba(0, 0, 0, 0.2)',
      backdropFilter: 'blur(10px)',
      position: 'sticky',
      top: 0,
      zIndex: 100
    }}>
      <Link href="/" style={{ textDecoration: 'none', color: 'inherit' }}>
        <h1 style={{ fontSize: '1.5rem', margin: 0 }}>
          Truth<span className="text-neon-purple">Lens</span>
        </h1>
      </Link>
      
      {session && (
        <>
          <nav style={{ display: 'flex', gap: '2rem' }}>
            <Link href="/dashboard" style={{ textDecoration: 'none', color: 'white', fontWeight: 500, opacity: 0.9 }}>Dashboard</Link>
            <Link href="/analyze" style={{ textDecoration: 'none', color: 'white', fontWeight: 500, opacity: 0.9 }}>Analyze</Link>
            <Link href="/history" style={{ textDecoration: 'none', color: 'white', fontWeight: 500, opacity: 0.9 }}>History</Link>
          </nav>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
            <span style={{ fontSize: '1rem', opacity: 0.9 }}>
              Welcome, <span className="text-neon-orange">{session.user?.name || session.user?.email || 'Użytkowniku'}</span>
            </span>
            <form action={async () => {
              "use server"
              await signOut({ redirectTo: '/' });
            }}>
              <button type="submit" className="glass" style={{
                padding: '0.4rem 0.8rem',
                background: 'rgba(255, 255, 255, 0.05)',
                color: 'white',
                border: '1px solid rgba(255,255,255,0.2)',
                borderRadius: '8px',
                cursor: 'pointer',
                fontSize: '0.9rem'
              }}>
                Sign Out
              </button>
            </form>
          </div>
        </>
      )}
      {!session && (
         <div style={{ display: 'flex', alignItems: 'center' }}>
            <Link href="/auth" style={{ textDecoration: 'none', color: 'white', fontWeight: 500, opacity: 0.9 }}>Log In</Link>
         </div>
      )}
    </header>
  );
}
