import { auth } from '@/auth';
import AnalyzeClient from './AnalyzeClient';

export default async function AnalyzePage() {
  const session = await auth();
  const token = (session as any)?.accessToken || '';

  return <AnalyzeClient token={token} />;
}
