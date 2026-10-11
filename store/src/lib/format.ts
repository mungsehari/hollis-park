import type { CartLine, Product, Promo, ShipMethod, StoreConfig, Width } from './types';


export function makeMoney(config: StoreConfig) {
  return (n: number) =>
    new Intl.NumberFormat(config.locale, {
      style: 'currency',
      currency: config.currency,
      minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
      maximumFractionDigits: 2,
    }).format(n);
}

export const pct = (r: number) => Math.round(r * 100) + '%';

export const countWord = (n: number) =>
  ['no', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve'][n] || String(n);

export const unitPrice = (p: Product, width: Width) => p.price + (width === 'wide' && p.wide ? p.wide.extra : 0);

export function inStock(p: Product, size: number, width: Width = 'standard') {
  if (width === 'wide') return !!p.wide && !p.wide.soldOut.includes(size) && !p.soldOut.includes(size);
  return !p.soldOut.includes(size);
}

export function addWorkingDays(from: Date, n: number) {
  const d = new Date(from.getTime());
  while (n > 0) {
    d.setDate(d.getDate() + 1);
    const w = d.getDay();
    if (w !== 0 && w !== 6) n--;
  }
  return d;
}

export function etaText(kind: 'standard' | 'express', locale: string) {
  const fmt = (d: Date) => d.toLocaleDateString(locale, { weekday: 'short', month: 'short', day: 'numeric' });
  const now = new Date();
  const lateOrWeekend = now.getHours() >= 14 || now.getDay() === 0 || now.getDay() === 6;
  const start = lateOrWeekend ? 1 : 0;
  if (kind === 'express') return 'Arrives ' + fmt(addWorkingDays(now, start + 1));
  return 'Arrives ' + fmt(addWorkingDays(now, start + 3)) + ' to ' + fmt(addWorkingDays(now, start + 5));
}

const round2 = (n: number) => Math.round(n * 100) / 100;

/** Mirrors the API's PricingService; the server recalculates when the order is placed. */
export function totals(
  cart: CartLine[],
  byId: Record<string, Product>,
  config: StoreConfig,
  promo: Promo | null,
  ship: ShipMethod = 'standard',
) {
  const items = cart.reduce((n, l) => n + l.qty, 0);
  const subtotal = cart.reduce((n, l) => n + unitPrice(byId[l.id], l.width) * l.qty, 0);
  const discount = promo ? round2(subtotal * promo.rate) : 0;
  let shipping = 0;
  if (items) {
    if (ship === 'express') shipping = config.expressFee;
    else if (ship === 'pickup') shipping = 0;
    else shipping = subtotal >= config.freeShipFrom ? 0 : config.shipFee;
  }
  const gross = round2(subtotal - discount + shipping);
  const tax = !config.taxRate
    ? 0
    : config.taxMode === 'included'
      ? round2((gross * config.taxRate) / (1 + config.taxRate))
      : round2((subtotal - discount) * config.taxRate);
  return { items, subtotal, discount, shipping, tax, total: config.taxMode === 'included' ? gross : round2(gross + tax) };
}

export function matchesQuery(p: Product, q: string, colourLabels: Record<string, { label: string }>) {
  const hay = `${p.name} ${p.colourway} ${p.collection} ${p.line} ${p.blurb} ${p.keywords} ${p.colours
    .map((c) => colourLabels[c]?.label ?? '')
    .join(' ')}`.toLowerCase();
  return q.toLowerCase().split(/\s+/).filter(Boolean).every((t) => hay.includes(t));
}

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export const reduceMotion = () =>
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

export const wait = (ms: number) => new Promise((r) => setTimeout(r, reduceMotion() ? 0 : ms));
