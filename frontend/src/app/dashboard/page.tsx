import { auth, signOut } from '@/auth';
import { redirect } from 'next/navigation';
import GlassTile from '@/components/GlassTile';
import Link from 'next/link';

export default async function DashboardPage() {
  const session = await auth();

  if (!session) {
    redirect('/auth');
  }

  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto', minHeight: '100vh' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '2rem' }}>Your Dashboard</h2>
        <p style={{ opacity: 0.8 }}>Select an action to start or view history.</p>
      </div>

      <section style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
        gap: '2rem',
        maxWidth: '800px',
        margin: '0 auto'
      }}>
        <GlassTile
          title="Analyze"
          description="Upload a screenshot of an article or social media post to verify its truthfulness."
          icon="🔍"
          color="purple"
          href="/analyze"
          actionText="Analyze"
        />
        <GlassTile
          title="History"
          description="View history of your previous analyses and their results."
          icon="📚"
          color="orange"
          href="/history"
          actionText="History"
        />
      </section>
    </div>
  );
}
