import React from 'react';
import {
  TrendingUp,
  Layers,
  ShoppingBag,
  Landmark,
  Building,
  ShieldCheck,
  Activity,
  Zap,
  Radio,
  Flame,
  Factory,
  Cpu,
} from 'lucide-react';

interface SectorIconProps {
  symbol: string;
  className?: string;
  size?: number;
}

export const SectorIcon: React.FC<SectorIconProps> = ({ symbol, className = 'w-4 h-4', size }) => {
  const props = { className, size: size || 16 };

  switch (symbol) {
    case 'SPX':
    case 'NDX':
      return <TrendingUp {...props} />;
    case '^YH101': // Basic Materials
      return <Layers {...props} />;
    case '^YH102': // Consumer Cyclical
      return <ShoppingBag {...props} />;
    case '^YH103': // Financial Services
      return <Landmark {...props} />;
    case '^YH104': // Real Estate
      return <Building {...props} />;
    case '^YH205': // Consumer Defensive
      return <ShieldCheck {...props} />;
    case '^YH206': // Healthcare
      return <Activity {...props} />;
    case '^YH207': // Utilities
      return <Zap {...props} />;
    case '^YH308': // Communication Services
      return <Radio {...props} />;
    case '^YH309': // Energy
      return <Flame {...props} />;
    case '^YH310': // Industrials
      return <Factory {...props} />;
    case '^YH311': // Technology
      return <Cpu {...props} />;
    default:
      return <TrendingUp {...props} />;
  }
};
