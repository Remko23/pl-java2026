import MagnifyingGlassAuth from '@/components/MagnifyingGlassAuth';
import { auth } from '@/auth';
import { redirect } from 'next/navigation';

export default async function AuthPage() {
  const session = await auth();

  if (session) {
    redirect('/dashboard');
  }

  return (
    <div style={{ 
      minHeight: '80vh', 
      display: 'flex', 
      flexDirection: 'column',
      alignItems: 'center', 
      justifyContent: 'center',
      padding: '2rem'
    }}>
      
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
