import GlassTile from '@/components/GlassTile';
import Link from 'next/link';
import { auth } from '@/auth';
import { redirect } from 'next/navigation';

export default async function Home() {
  const session = await auth();

  if (session) {
    redirect('/dashboard');
  }

  return (
    <main style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{
        padding: '2rem 0 1rem 0',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        gap: '1rem'
      }}>
        <h1 style={{ fontSize: '4rem', marginBottom: '0.1rem' }}>
          Truth<span className="text-neon-purple">Lens</span>
        </h1>
        <p style={{ fontSize: '1.2rem', maxWidth: '600px', opacity: 0.9 }}>
          AI - powered truth verification system.
        </p>
      </header>

      <section style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
        gap: '3rem',
        padding: '2rem 0',
        maxWidth: '1000px',
        margin: '0 auto'
      }}>
        <GlassTile
          title="Analyze"
          description="Upload a screenshot of an article or social media post to verify its truthfulness."
          icon="🔍"
          color="purple"
          href="/analyze"
          actionText="Get Started"
        />
        <GlassTile
          title="Log In"
          description="Access your dashboard to find the history of your analyses and detailed verification reports."
          icon="🔐"
          color="orange"
          href="/auth"
          actionText="Login Panel"
        />
      </section>

      <footer style={{ textAlign: 'center', padding: '0.4rem 0', opacity: 0.5, fontSize: '0.9rem' }}>
        "We cannot change the past, but we can improve for the future." ~ Sensei Wu
        <br />
        TruthLens.
      </footer>
    </main>
  );
}
