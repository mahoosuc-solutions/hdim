import React from 'react';
import { AbsoluteFill } from 'remotion';

export const TestSimple: React.FC = () => {
  return (
    <AbsoluteFill
      style={{
        backgroundColor: '#0066CC',
        justifyContent: 'center',
        alignItems: 'center',
        fontSize: '60px',
        color: 'white',
      }}
    >
      Hello Remotion!
    </AbsoluteFill>
  );
};
