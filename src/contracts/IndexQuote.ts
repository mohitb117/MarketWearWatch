/**
 * Shared IndexQuote data contract.
 * Direct TypeScript binding for com.marketwear.shared.model.IndexQuote.kt
 */
export type InstrumentCategory = 'INDEX' | 'SECTOR' | 'PORTFOLIO';

export interface IndexQuoteData {
  symbol: string;        // "SPX", "NDX", "^YH101"..."^YH311"
  name: string;          // "S&P 500", "Basic Materials"
  price: number;         // e.g. 5985.42
  change: number;        // e.g. +22.84
  percentChange: number; // e.g. +0.38
  dayHigh: number;       // e.g. 6005.10
  dayLow: number;        // e.g. 5960.30
  lastUpdated: number;   // Unix timestamp millis
  category?: InstrumentCategory;
  sparkline?: number[];
  volume?: string;
}

export class IndexQuote {
  readonly symbol: string;
  readonly name: string;
  readonly price: number;
  readonly change: number;
  readonly percentChange: number;
  readonly dayHigh: number;
  readonly dayLow: number;
  readonly lastUpdated: number;
  readonly category: InstrumentCategory;
  readonly sparkline: number[];
  readonly volume: string;

  constructor(data: IndexQuoteData) {
    this.symbol = data.symbol;
    this.name = data.name;
    this.price = data.price;
    this.change = data.change;
    this.percentChange = data.percentChange;
    this.dayHigh = data.dayHigh;
    this.dayLow = data.dayLow;
    this.lastUpdated = data.lastUpdated;
    this.category = data.category || (data.symbol === 'PORTFOLIO' ? 'PORTFOLIO' : data.symbol.startsWith('^YH') ? 'SECTOR' : 'INDEX');
    this.sparkline = data.sparkline || [];
    this.volume = data.volume || '--';
  }

  get isPositive(): boolean {
    return this.change >= 0;
  }

  get isPortfolio(): boolean {
    return this.category === 'PORTFOLIO' || this.symbol === 'PORTFOLIO';
  }

  get formattedScore(): string {
    return this.price.toFixed(2);
  }

  get formattedPoints(): string {
    if (this.isPortfolio) {
      return this.formattedScore;
    }
    return Math.round(this.price).toLocaleString();
  }

  get formattedChange(): string {
    const sign = this.isPositive ? '+' : '';
    return `${sign}${this.change.toFixed(2)}`;
  }

  get formattedPercent(): string {
    const sign = this.isPositive ? '+' : '';
    return `${sign}${this.percentChange.toFixed(2)}%`;
  }

  /**
   * Compact number format mirroring Android Kotlin logic:
   * >= 1,000,000 -> "2.22M"
   * >= 100,000   -> "185k"
   * >= 10,000    -> "34.3k"
   * >= 1,000     -> "1.12k"
   * else         -> "598.25"
   */
  get formattedCompactPoints(): string {
    return IndexQuote.formatCompactNumber(this.price);
  }

  get formattedCompactChange(): string {
    const sign = this.isPositive ? '+' : '-';
    return `${sign}${IndexQuote.formatCompactNumber(Math.abs(this.change))}`;
  }

  static formatCompactNumber(value: number): string {
    const abs = Math.abs(value);
    if (abs >= 1_000_000) {
      return `${(abs / 1_000_000).toFixed(2)}M`;
    } else if (abs >= 100_000) {
      return `${(abs / 1_000).toFixed(0)}k`;
    } else if (abs >= 10_000) {
      return `${(abs / 1_000).toFixed(1)}k`;
    } else if (abs >= 1_000) {
      return `${(abs / 1_000).toFixed(2)}k`;
    } else {
      return abs.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }
  }
}
