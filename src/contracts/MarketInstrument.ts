import { IndexQuote } from './IndexQuote';

export interface MarketInstrument {
  symbol: string;
  yahooTicker: string;
  name: string;
  category: 'INDEX' | 'SECTOR';
  icon: string;
}

export const MarketRegistry = {
  SPX: { symbol: 'SPX', yahooTicker: '^GSPC', name: 'S&P 500', category: 'INDEX', icon: 'ic_sp500' },
  NDX: { symbol: 'NDX', yahooTicker: '^NDX', name: 'Nasdaq 100', category: 'INDEX', icon: 'ic_nasdaq' },

  YH101: { symbol: '^YH101', yahooTicker: '^YH101', name: 'Basic Materials', category: 'SECTOR', icon: 'ic_sector_materials' },
  YH102: { symbol: '^YH102', yahooTicker: '^YH102', name: 'Consumer Cyclical', category: 'SECTOR', icon: 'ic_sector_cyclical' },
  YH103: { symbol: '^YH103', yahooTicker: '^YH103', name: 'Financial Services', category: 'SECTOR', icon: 'ic_sector_financial' },
  YH104: { symbol: '^YH104', yahooTicker: '^YH104', name: 'Real Estate', category: 'SECTOR', icon: 'ic_sector_realestate' },
  YH205: { symbol: '^YH205', yahooTicker: '^YH205', name: 'Consumer Defensive', category: 'SECTOR', icon: 'ic_sector_defensive' },
  YH206: { symbol: '^YH206', yahooTicker: '^YH206', name: 'Healthcare', category: 'SECTOR', icon: 'ic_sector_healthcare' },
  YH207: { symbol: '^YH207', yahooTicker: '^YH207', name: 'Utilities', category: 'SECTOR', icon: 'ic_sector_utilities' },
  YH308: { symbol: '^YH308', yahooTicker: '^YH308', name: 'Communication Services', category: 'SECTOR', icon: 'ic_sector_communication' },
  YH309: { symbol: '^YH309', yahooTicker: '^YH309', name: 'Energy', category: 'SECTOR', icon: 'ic_sector_energy' },
  YH310: { symbol: '^YH310', yahooTicker: '^YH310', name: 'Industrials', category: 'SECTOR', icon: 'ic_sector_industrials' },
  YH311: { symbol: '^YH311', yahooTicker: '^YH311', name: 'Technology', category: 'SECTOR', icon: 'ic_sector_technology' },
} as const;

export const ALL_INSTRUMENTS: readonly MarketInstrument[] = [
  MarketRegistry.SPX,
  MarketRegistry.NDX,
  MarketRegistry.YH101,
  MarketRegistry.YH102,
  MarketRegistry.YH103,
  MarketRegistry.YH104,
  MarketRegistry.YH205,
  MarketRegistry.YH206,
  MarketRegistry.YH207,
  MarketRegistry.YH308,
  MarketRegistry.YH309,
  MarketRegistry.YH310,
  MarketRegistry.YH311,
];

export const INDICES = ALL_INSTRUMENTS.filter(i => i.category === 'INDEX');
export const SECTORS = ALL_INSTRUMENTS.filter(i => i.category === 'SECTOR');

export function getFallbackQuotes(): IndexQuote[] {
  const now = Date.now();
  return [
    new IndexQuote({ symbol: 'SPX', name: 'S&P 500', price: 5985.42, change: 22.84, percentChange: 0.38, dayHigh: 6005.10, dayLow: 5960.30, lastUpdated: now, category: 'INDEX', sparkline: [5960, 5970, 5965, 5980, 5985] }),
    new IndexQuote({ symbol: 'NDX', name: 'Nasdaq 100', price: 21110.15, change: -45.20, percentChange: -0.21, dayHigh: 21250.00, dayLow: 21050.80, lastUpdated: now, category: 'INDEX', sparkline: [21155, 21180, 21120, 21090, 21110] }),
    new IndexQuote({ symbol: '^YH101', name: 'Basic Materials', price: 2220463.0, change: 34300.0, percentChange: 1.57, dayHigh: 2250000.0, dayLow: 2190000.0, lastUpdated: now, category: 'SECTOR', sparkline: [2186, 2195, 2204, 2210, 2220] }),
    new IndexQuote({ symbol: '^YH102', name: 'Consumer Cyclical', price: 1850120.0, change: 12400.0, percentChange: 0.67, dayHigh: 1870000.0, dayLow: 1830000.0, lastUpdated: now, category: 'SECTOR', sparkline: [1837, 1842, 1845, 1848, 1850] }),
    new IndexQuote({ symbol: '^YH103', name: 'Financial Services', price: 1420500.0, change: -8200.0, percentChange: -0.57, dayHigh: 1440000.0, dayLow: 1410000.0, lastUpdated: now, category: 'SECTOR', sparkline: [1428, 1432, 1425, 1421, 1420] }),
    new IndexQuote({ symbol: '^YH104', name: 'Real Estate', price: 512300.0, change: 2100.0, percentChange: 0.41, dayHigh: 520000.0, dayLow: 508000.0, lastUpdated: now, category: 'SECTOR', sparkline: [510, 511, 509, 513, 512] }),
    new IndexQuote({ symbol: '^YH205', name: 'Consumer Defensive', price: 980400.0, change: -1500.0, percentChange: -0.15, dayHigh: 990000.0, dayLow: 975000.0, lastUpdated: now, category: 'SECTOR', sparkline: [981, 983, 980, 979, 980] }),
    new IndexQuote({ symbol: '^YH206', name: 'Healthcare', price: 1650300.0, change: 8900.0, percentChange: 0.54, dayHigh: 1670000.0, dayLow: 1640000.0, lastUpdated: now, category: 'SECTOR', sparkline: [1641, 1646, 1648, 1652, 1650] }),
    new IndexQuote({ symbol: '^YH207', name: 'Utilities', price: 420800.0, change: 1100.0, percentChange: 0.26, dayHigh: 425000.0, dayLow: 418000.0, lastUpdated: now, category: 'SECTOR', sparkline: [419, 421, 418, 420, 420] }),
    new IndexQuote({ symbol: '^YH308', name: 'Communication Services', price: 1280900.0, change: 14500.0, percentChange: 1.14, dayHigh: 1295000.0, dayLow: 1270000.0, lastUpdated: now, category: 'SECTOR', sparkline: [1266, 1272, 1278, 1282, 1280] }),
    new IndexQuote({ symbol: '^YH309', name: 'Energy', price: 890200.0, change: -6400.0, percentChange: -0.71, dayHigh: 905000.0, dayLow: 885000.0, lastUpdated: now, category: 'SECTOR', sparkline: [896, 894, 891, 888, 890] }),
    new IndexQuote({ symbol: '^YH310', name: 'Industrials', price: 1750600.0, change: 9300.0, percentChange: 0.53, dayHigh: 1770000.0, dayLow: 1740000.0, lastUpdated: now, category: 'SECTOR', sparkline: [1741, 1746, 1749, 1752, 1750] }),
    new IndexQuote({ symbol: '^YH311', name: 'Technology', price: 27007258.0, change: 290900.0, percentChange: 1.09, dayHigh: 27300000.0, dayLow: 26800000.0, lastUpdated: now, category: 'SECTOR', sparkline: [26716, 26820, 26910, 27050, 27007] }),
  ];
}
