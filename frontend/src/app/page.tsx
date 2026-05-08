import GlassTile from '@/components/GlassTile';
import Link from 'next/link';

export default function Home() {
  return (
    <main style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{
        height: '50vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        gap: '1.5rem'
      }}>
        <h1 style={{ fontSize: '4rem', marginBottom: '1rem' }}>
          Truth<span className="text-neon-purple">Lens</span>
        </h1>
        <p style={{ fontSize: '1.25rem', maxWidth: '600px', opacity: 0.9 }}>
          AI - powered truth verification system.
        </p>
      </header>

      <section style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
        gap: '2rem',
        padding: '4rem 0',
        maxWidth: '800px',
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

      <footer style={{ textAlign: 'center', padding: '4rem 0', opacity: 0.5, fontSize: '0.9rem' }}>
        "We cannot change the past, but we can improve for the future." ~ Sensei Wu
        <br />
        TruthLens.
      </footer>
    </main>
  );
}
