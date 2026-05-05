import TransferPageClient from './TransferPageClient';

export default async function TransferPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const params = await searchParams;
  return <TransferPageClient token={params.token ?? ''} />;
}
