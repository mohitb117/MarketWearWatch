import { IndexQuote, IndexQuoteData, InstrumentCategory } from '../contracts/IndexQuote';
import {
  ALL_INSTRUMENTS,
  getFallbackQuotes,
  MarketRegistry,
  toUnnormalizedPortfolioQuote,
  toUnnormalizedPortfolioScore,
  TOTAL_RAW_PORTFOLIO_WEIGHT_PCT,
} from '../contracts/MarketInstrument';

export {
  IndexQuote,
  MarketRegistry,
  ALL_INSTRUMENTS,
  toUnnormalizedPortfolioQuote,
  toUnnormalizedPortfolioScore,
  TOTAL_RAW_PORTFOLIO_WEIGHT_PCT,
};
export type { InstrumentCategory, IndexQuoteData };

export type MarketIndex = IndexQuote;

export type MarketStatus = 'OPEN' | 'CLOSED' | 'PRE_MARKET' | 'AFTER_HOURS';

export interface MarketState {
  quotes: IndexQuote[];
  spx: IndexQuote;
  ndx: IndexQuote;
  sectors: Record<string, IndexQuote>;
  status: MarketStatus;
  statusText: string;
  lastSyncTime: Date;
  syncSource: 'data_layer' | 'standalone' | 'cached';
  isWatchConnected: boolean;
}

const initialQuotes = getFallbackQuotes();
const initialSpx = initialQuotes.find(q => q.symbol === 'SPX')!;
const initialNdx = initialQuotes.find(q => q.symbol === 'NDX')!;
const initialSectorsMap = initialQuotes
  .filter(q => q.category === 'SECTOR')
  .reduce((acc, q) => {
    acc[q.symbol] = q;
    return acc;
  }, {} as Record<string, IndexQuote>);

export const INITIAL_MARKET_STATE: MarketState = {
  quotes: initialQuotes,
  spx: initialSpx,
  ndx: initialNdx,
  sectors: initialSectorsMap,
  status: 'OPEN',
  statusText: 'US Regular Trading Session',
  lastSyncTime: new Date(),
  syncSource: 'data_layer',
  isWatchConnected: true,
};

export function getComplicationText(
  quote: IndexQuote,
  format: 'points' | 'percent' | 'change' | 'compact'
): string {
  switch (format) {
    case 'points':
      return quote.formattedPoints;
    case 'percent':
      return quote.formattedPercent;
    case 'change':
      return quote.formattedChange;
    case 'compact':
      return quote.formattedCompactPoints;
  }
}

export function calculateMarketStatus(): { status: MarketStatus; statusText: string } {
  const now = new Date();
  const utcHours = now.getUTCHours();
  const utcMinutes = now.getUTCMinutes();
  const day = now.getUTCDay();

  if (day === 0 || day === 6) {
    return {
      status: 'CLOSED',
      statusText: 'Market Closed (Weekend)',
    };
  }

  const currentUtcMinutes = utcHours * 60 + utcMinutes;
  const regularOpenUtc = 14 * 60 + 30; // 9:30 AM EST
  const regularCloseUtc = 21 * 60; // 4:00 PM EST

  if (currentUtcMinutes >= regularOpenUtc && currentUtcMinutes <= regularCloseUtc) {
    return {
      status: 'OPEN',
      statusText: 'US Regular Trading Session',
    };
  } else if (currentUtcMinutes < regularOpenUtc && currentUtcMinutes >= regularOpenUtc - 330) {
    return {
      status: 'PRE_MARKET',
      statusText: 'US Pre-Market Session',
    };
  } else {
    return {
      status: 'AFTER_HOURS',
      statusText: 'US After-Hours / Closed',
    };
  }
}
