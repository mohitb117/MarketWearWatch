export interface MarketIndex {
  symbol: 'SPX' | 'NDX';
  name: string;
  ticker: string;
  price: number;
  change: number;
  percentChange: number;
  dayHigh: number;
  dayLow: number;
  open: number;
  prevClose: number;
  volume: string;
  fiftyTwoWeekHigh: number;
  fiftyTwoWeekLow: number;
  sparkline: number[];
  lastUpdated: string;
}

export type MarketStatus = 'OPEN' | 'CLOSED' | 'PRE_MARKET' | 'AFTER_HOURS';

export interface MarketState {
  spx: MarketIndex;
  ndx: MarketIndex;
  status: MarketStatus;
  statusText: string;
  lastSyncTime: Date;
}

export const INITIAL_MARKET_STATE: MarketState = {
  spx: {
    symbol: 'SPX',
    name: 'S&P 500',
    ticker: '^GSPC',
    price: 5983.25,
    change: 36.80,
    percentChange: 0.62,
    dayHigh: 6005.10,
    dayLow: 5932.40,
    open: 5946.45,
    prevClose: 5946.45,
    volume: '2.41B',
    fiftyTwoWeekHigh: 6025.50,
    fiftyTwoWeekLow: 4953.56,
    sparkline: [5946, 5952, 5940, 5961, 5975, 5968, 5980, 5992, 5983],
    lastUpdated: 'Just now'
  },
  ndx: {
    symbol: 'NDX',
    name: 'Nasdaq 100',
    ticker: '^NDX',
    price: 21394.50,
    change: 248.90,
    percentChange: 1.18,
    dayHigh: 21475.20,
    dayLow: 21150.80,
    open: 21145.60,
    prevClose: 21145.60,
    volume: '4.88B',
    fiftyTwoWeekHigh: 21600.00,
    fiftyTwoWeekLow: 16973.12,
    sparkline: [21145, 21190, 21240, 21210, 21320, 21350, 21420, 21394],
    lastUpdated: 'Just now'
  },
  status: 'OPEN',
  statusText: 'US Markets Open (NYSE & Nasdaq)',
  lastSyncTime: new Date()
};

export function getComplicationText(
  quote: MarketIndex,
  format: 'points' | 'percent' | 'change' | 'compact'
): string {
  switch (format) {
    case 'points':
      return Math.round(quote.price).toLocaleString();
    case 'percent':
      return `${quote.change >= 0 ? '+' : ''}${quote.percentChange.toFixed(2)}%`;
    case 'change':
      return `${quote.change >= 0 ? '+' : ''}${quote.change.toFixed(1)}`;
    case 'compact':
      return `${(quote.price / 1000).toFixed(2)}k`;
  }
}

export function calculateMarketStatus(): { status: MarketStatus; statusText: string } {
  // Eastern Time zone for US financial markets
  const now = new Date();
  const utcHours = now.getUTCHours();
  const utcMinutes = now.getUTCMinutes();
  const day = now.getUTCDay(); // 0 is Sunday, 6 is Saturday

  // If weekend
  if (day === 0 || day === 6) {
    return {
      status: 'CLOSED',
      statusText: 'Market Closed (Weekend)'
    };
  }

  // NYSE/Nasdaq regular trading hours: 9:30 AM - 4:00 PM EST (14:30 - 21:00 UTC during EST, or 13:30 - 20:00 UTC during EDT)
  const currentUtcMinutes = utcHours * 60 + utcMinutes;
  const regularOpenUtc = 14 * 60 + 30; // 9:30 AM EST
  const regularCloseUtc = 21 * 60; // 4:00 PM EST

  if (currentUtcMinutes >= regularOpenUtc && currentUtcMinutes <= regularCloseUtc) {
    return {
      status: 'OPEN',
      statusText: 'US Regular Trading Session'
    };
  } else if (currentUtcMinutes < regularOpenUtc && currentUtcMinutes >= regularOpenUtc - 330) {
    return {
      status: 'PRE_MARKET',
      statusText: 'US Pre-Market Session'
    };
  } else {
    return {
      status: 'AFTER_HOURS',
      statusText: 'US After-Hours / Closed'
    };
  }
}
