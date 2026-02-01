import React from 'react';
import { AbsoluteFill } from 'remotion';

interface BrandedContainerProps {
  children: React.ReactNode;
  variant?: 'blue-teal' | 'dark-blue' | 'light';
  opacity?: number;
}

export const BrandedContainer: React.FC<BrandedContainerProps> = ({
  children,
  variant = 'blue-teal',
  opacity = 1,
}) => {
  const gradients = {
    'blue-teal': 'linear-gradient(135deg, #0066CC 0%, #00CC88 100%)',
    'dark-blue': 'linear-gradient(135deg, #003D7A 0%, #0066CC 100%)',
    'light': 'linear-gradient(135deg, #00CC88 0%, #66D9B8 100%)',
  };

  return (
    <AbsoluteFill
      style={{
        background: gradients[variant],
        opacity,
      }}
    >
      {children}
    </AbsoluteFill>
  );
};
