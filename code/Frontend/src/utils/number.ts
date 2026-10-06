export function formatMoney(
  amount: number | string | null | undefined = 0,
  currency = 'THB',
): string {
  return new Intl.NumberFormat('th-TH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(Number(amount) || 0)
}
