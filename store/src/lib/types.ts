export type Width = 'standard' | 'wide';
export type ShipMethod = 'standard' | 'express' | 'pickup';

export interface Product {
  id: string;
  name: string;
  collection: string;
  line: string;
  colourway: string;
  swatch: string;
  colours: string[];
  price: number;
  wide: { extra: number; soldOut: number[] } | null;
  rating: number;
  reviews: number;
  flag: string | null;
  rank: number;
  added: string;
  img: string;
  gallery: string[];
  context: string | null;
  contextCaption?: string;
  alt: string;
  blurb: string;
  desc: string;
  specs: [string, string, string][];
  materials: [string, string][];
  fit: { verdict: string; text: string };
  care: string;
  soldOut: number[];
  low: number[];
  keywords: string;
  /** The same pair on a marketplace; reached through the store's counted redirect. */
  amazonUrl?: string | null;
  flipkartUrl?: string | null;
}

export interface Review {
  product: string;
  name: string;
  place: string;
  size: string;
  fit: number;
  rating: number;
  date: string;
  text: string;
}

export interface Collection {
  id: string;
  name: string;
  image: string | null;
  alt: string | null;
  tagline: string;
  intro: string;
  choose: string[];
  guide: { icon: string; t: string; p: string }[];
  sortOrder: number;
}

export interface HomeContent {
  hero: {
    eyebrow?: string; title: string; subtitle?: string; image?: string | null; imageAlt?: string;
    ctaLabel?: string; ctaLink?: string; secondaryLabel?: string; secondaryLink?: string;
  };
  banner: { enabled: boolean; text?: string; link?: string };
  spotlightProductId?: string;
}

export interface Line {
  id: string;
  name: string;
  for: string;
  text: string;
  products: string[];
}

export interface FaqItem {
  cat: string;
  home?: boolean;
  q: string;
  a: string;
}

export interface StoreConfig {
  locale: string;
  currency: string;
  taxMode: 'included' | 'added';
  taxRate: number;
  taxName: string;
  freeShipFrom: number;
  shipFee: number;
  expressFee: number;
  sizeLabel: string;
  postcode: { label: string; pattern: string; hint: string; numeric: boolean };
}

export type Marketplace = 'AMAZON' | 'FLIPKART';

/** Something that goes with the shoes, bought on a marketplace (affiliate link). */
export interface CarePick {
  id: string;
  title: string;
  blurb?: string | null;
  image?: string | null;
  marketplace: Marketplace;
  marketplaceLabel: string;
}

export interface StoreData {
  site: { name: string; store: string; orderPrefix: string };
  payments?: { enabled: boolean; testMode: boolean };
  home: HomeContent;
  config: StoreConfig;
  collections: Collection[];
  lines: Line[];
  faq: FaqItem[];
  faqCats: [string, string][];
  sizeChart: [number, number, number, number][];
  sizes: number[];
  colours: Record<string, { label: string; hex: string }>;
  products: Product[];
  reviews: Review[];
  picks: CarePick[];
}

export interface CartLine {
  key: string;
  id: string;
  size: number;
  width: Width;
  qty: number;
}

export interface Promo {
  code: string;
  rate: number;
}
