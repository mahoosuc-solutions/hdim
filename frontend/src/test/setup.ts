import '@testing-library/jest-dom';
import React from 'react';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

// Mock localStorage
class LocalStorageMock {
  private store: Map<string, string> = new Map();

  getItem(key: string): string | null {
    return this.store.get(key) || null;
  }

  setItem(key: string, value: string): void {
    this.store.set(key, value);
  }

  removeItem(key: string): void {
    this.store.delete(key);
  }

  clear(): void {
    this.store.clear();
  }

  get length(): number {
    return this.store.size;
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] || null;
  }
}

globalThis.localStorage = new LocalStorageMock() as Storage;

// Mock matchMedia for dark mode detection
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => true,
  }),
});

// Mock ResizeObserver for Recharts
globalThis.ResizeObserver = class ResizeObserver {
  constructor(callback: ResizeObserverCallback) {
    // Immediately call the callback with mock dimensions
    setTimeout(() => {
      callback(
        [
          {
            target: document.createElement('div'),
            contentRect: {
              width: 800,
              height: 400,
              top: 0,
              left: 0,
              bottom: 400,
              right: 800,
              x: 0,
              y: 0,
              toJSON: () => ({}),
            },
            borderBoxSize: [],
            contentBoxSize: [],
            devicePixelContentBoxSize: [],
          } as ResizeObserverEntry,
        ],
        this
      );
    }, 0);
  }
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Provide basic element sizing so components that rely on layout (charts, sliders) don't get 0x0
Object.defineProperties(HTMLElement.prototype, {
  offsetHeight: {
    get() {
      return 400;
    },
  },
  offsetWidth: {
    get() {
      return 800;
    },
  },
  getBoundingClientRect: {
    value() {
      return {
        width: 800,
        height: 400,
        top: 0,
        left: 0,
        right: 800,
        bottom: 400,
        x: 0,
        y: 0,
        toJSON: () => ({}),
      };
    },
  },
  clientWidth: {
    get() {
      return 800;
    },
  },
  clientHeight: {
    get() {
      return 400;
    },
  },
});

// Ensure computed styles return dimensions for chart containers
globalThis.getComputedStyle = ((orig) => (elt: Element) => {
  const style = orig ? orig(elt) : ({} as CSSStyleDeclaration);
  const fallback = { width: '800px', height: '400px' } as Record<string, string>;
  return {
    ...style,
    width: style.width || '800px',
    height: style.height || '400px',
    getPropertyValue: (prop: string) => (style as any).getPropertyValue?.(prop) ?? fallback[prop] ?? '',
  };
})(globalThis.getComputedStyle);

// Mock URL.createObjectURL and URL.revokeObjectURL for file downloads
globalThis.URL.createObjectURL = vi.fn(() => 'mock-object-url');
globalThis.URL.revokeObjectURL = vi.fn();

// Simplify Recharts ResponsiveContainer to avoid layout-dependent sizing in jsdom
vi.mock('recharts', () => {
  const ResponsiveContainer = ({ children }: any) => {
    const content =
      typeof children === 'function'
        ? children({ width: 800, height: 400 })
        : children;
    return React.createElement(
      'div',
      {
        'data-testid': 'responsive-container',
        className: 'recharts-responsive-container',
        style: { width: '800px', height: '400px' },
      },
      content
    );
  };

  const BarChart = ({ children, data }: any) => {
    const keyedChildren = React.Children.map(children, (child, idx) =>
      React.isValidElement(child) ? React.cloneElement(child, { key: child.key ?? idx }) : child
    );
    return React.createElement(
      'div',
      { 'data-testid': 'bar-chart', className: 'recharts-wrapper recharts-bar-chart' },
      React.createElement(
        'div',
        { className: 'recharts-xAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement(
        'div',
        { className: 'recharts-yAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement('div', { className: 'recharts-tooltip-wrapper' }),
      data?.map((d: any, idx: number) => React.createElement('span', { key: d.range ?? idx }, d.range ?? '')),
      keyedChildren
    );
  };

  const LineChart = ({ children }: any) =>
    React.createElement(
      'div',
      { 'data-testid': 'line-chart', className: 'recharts-wrapper recharts-line-chart' },
      React.createElement(
        'div',
        { className: 'recharts-xAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement(
        'div',
        { className: 'recharts-yAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement('div', { className: 'recharts-tooltip-wrapper' }),
      React.createElement('div', { className: 'recharts-line' }),
      children
    );

  const AreaChart = ({ children }: any) =>
    React.createElement(
      'div',
      { 'data-testid': 'area-chart', className: 'recharts-wrapper recharts-area-chart' },
      React.createElement(
        'div',
        { className: 'recharts-xAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement(
        'div',
        { className: 'recharts-yAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement('div', { className: 'recharts-tooltip-wrapper' }),
      React.createElement('div', { className: 'recharts-area' }),
      children
    );

  const ComposedChart = ({ children }: any) =>
    React.createElement(
      'div',
      { 'data-testid': 'composed-chart', className: 'recharts-wrapper recharts-composed-chart' },
      React.createElement(
        'div',
        { className: 'recharts-xAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement(
        'div',
        { className: 'recharts-yAxis' },
        React.createElement('span', { className: 'recharts-cartesian-axis-tick recharts-text' }, 'tick')
      ),
      React.createElement('div', { className: 'recharts-tooltip-wrapper' }),
      children
    );

  const CartesianGrid = () => React.createElement('div', { 'data-testid': 'cartesian-grid' });
  const XAxis = ({ children, label }: any) =>
    React.createElement(
      'div',
      { 'data-testid': 'x-axis' },
      label?.value || label || null,
      children
    );
  const YAxis = ({ children, label }: any) => {
    const labelNode =
      label || label === 0
        ? React.createElement('span', { className: 'recharts-label' }, label?.value || label)
        : null;
    return React.createElement('div', { 'data-testid': 'y-axis', className: 'recharts-yAxis' }, labelNode, children);
  };
  const Tooltip = () =>
    React.createElement(
      'div',
      { 'data-testid': 'tooltip', className: 'recharts-tooltip-wrapper' },
      'tooltip'
    );
  const Legend = () => React.createElement('div', { 'data-testid': 'legend' }, 'legend');
  const ReferenceLine = ({ label }: any) =>
    React.createElement('div', { 'data-testid': 'reference-line' }, label?.value || label || 'Average');
  const Bar = ({ children }: any) =>
    React.createElement('div', { 'data-testid': 'bar', className: 'recharts-bar' }, children);
  const Cell = () => React.createElement('div', { 'data-testid': 'cell', className: 'recharts-cell' }, 'cell');
  const Line = ({ name, dataKey }: any) =>
    React.createElement('div', { 'data-testid': 'line', className: 'recharts-line' }, name || dataKey || 'value');
  const Area = ({ name, dataKey }: any) =>
    React.createElement('div', { 'data-testid': 'area', className: 'recharts-area' }, name || dataKey || 'value');
  const Label = ({ value }: any) => React.createElement('span', null, value);

  return {
    ResponsiveContainer,
    BarChart,
    LineChart,
    AreaChart,
    ComposedChart,
    CartesianGrid,
    XAxis,
    YAxis,
    Tooltip,
    Legend,
    ReferenceLine,
    Bar,
    Cell,
    Line,
    Area,
    Label,
  };
});

// Cleanup after each test
afterEach(() => {
  cleanup();
});
