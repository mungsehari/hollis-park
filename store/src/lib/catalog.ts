import type { Product } from './types';

/** Catalog price bands, in the store currency. */
export const PRICE_BANDS = [
  { id: 'under-10k', min: -Infinity, max: 10000, test: (n: number) => n < 10000 },
  { id: '10k-12k', min: 10000, max: 12000, test: (n: number) => n >= 10000 && n <= 12000 },
  { id: 'over-12k', min: 12000, max: Infinity, test: (n: number) => n > 12000 },
];

/** The "Under ₹12,000" tab on Home. */
export const VALUE_LIMIT = 12000;

export const POPULAR = {
  bestsellers: (ps: Product[]) => [...ps].sort((a, b) => a.rank - b.rank).slice(0, 4),
  new: (ps: Product[]) => [...ps].sort((a, b) => b.added.localeCompare(a.added)).slice(0, 4),
  value: (ps: Product[]) => ps.filter((p) => p.price < VALUE_LIMIT).sort((a, b) => a.price - b.price).slice(0, 4),
};

export const SORTS: Record<string, (a: Product, b: Product) => number> = {
  featured: (a, b) => a.rank - b.rank,
  new: (a, b) => b.added.localeCompare(a.added),
  'price-asc': (a, b) => a.price - b.price,
  'price-desc': (a, b) => b.price - a.price,
  rating: (a, b) => b.rating - a.rating || b.reviews - a.reviews,
};

export const PAGES: [string, string, string][] = [
  ['Size guide', 'size-guide', 'Measure your foot and convert sizes'],
  ['Delivery and returns', 'delivery', 'Shipping, the 60-day wear test, resoles'],
  ['Questions', 'faq', 'Sizing, orders, returns and care'],
  ['Contact and fittings', 'contact', 'Book a fitting at Marlowe'],
  ['Our story', 'about', 'Why eight models and a resole bench'],
  ['Our lines', 'brands', 'Tide Studio, Yard Supply, Hollis Form'],
];
