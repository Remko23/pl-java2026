import { auth } from '@/auth';
import { redirect } from 'next/navigation';
import HistoryClient from './HistoryClient';

export default async function HistoryPage() {
  const session = await auth();

  if (!session) {
    redirect('/auth');
  }

  const token = (session as any)?.accessToken || '';

  return <HistoryClient token={token} />;
}

