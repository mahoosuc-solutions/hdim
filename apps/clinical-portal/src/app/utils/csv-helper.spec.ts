import { CSVHelper } from './csv-helper';

describe('CSVHelper', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('escapes nullish values and plain strings', () => {
    expect(CSVHelper.escapeCSVValue(null)).toBe('');
    expect(CSVHelper.escapeCSVValue(undefined)).toBe('');
    expect(CSVHelper.escapeCSVValue('Plain')).toBe('Plain');
    expect(CSVHelper.escapeCSVValue(42)).toBe('42');
    expect(CSVHelper.escapeCSVValue(true)).toBe('true');
  });

  it('escapes values with commas, quotes, and newlines', () => {
    expect(CSVHelper.escapeCSVValue('Smith, John')).toBe('"Smith, John"');
    expect(CSVHelper.escapeCSVValue('Said "Hello"')).toBe('"Said ""Hello"""');
    expect(CSVHelper.escapeCSVValue('Line1\nLine2')).toBe('"Line1\nLine2"');
    expect(CSVHelper.escapeCSVValue('Line1\rLine2')).toBe('"Line1\rLine2"');
  });

  it('converts rows to CSV with escaped values', () => {
    const rows = [
      ['Name', 'Age', 'City'],
      ['Smith, John', 25, 'New York'],
      ['Doe, Jane', 30, 'Los Angeles'],
    ];
    const csv = CSVHelper.arrayToCSV(rows);
    expect(csv).toBe(
      'Name,Age,City\n"Smith, John",25,New York\n"Doe, Jane",30,Los Angeles'
    );
  });

  it('formats dates for CSV export', () => {
    expect(CSVHelper.formatDate(null)).toBe('');
    expect(CSVHelper.formatDate('invalid')).toBe('');
    const date = new Date('2025-01-02T03:04:05Z');
    const expected = CSVHelper.formatDate(date);
    expect(expected).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    expect(CSVHelper.formatDate(date)).toBe(expected);
    expect(CSVHelper.formatDate('2025-12-31T23:59:59Z')).toMatch(
      /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/
    );
  });

  it('formats percentages for CSV export', () => {
    expect(CSVHelper.formatPercentage(null)).toBe('');
    expect(CSVHelper.formatPercentage(NaN)).toBe('');
    expect(CSVHelper.formatPercentage(25)).toBe('25.0%');
    expect(CSVHelper.formatPercentage(0.257, true)).toBe('25.7%');
  });

  it('triggers a CSV download using a blob URL', () => {
    if (!('createObjectURL' in URL)) {
      Object.defineProperty(URL, 'createObjectURL', {
        value: () => 'blob:csv',
        configurable: true,
        writable: true,
      });
    }
    if (!('revokeObjectURL' in URL)) {
      Object.defineProperty(URL, 'revokeObjectURL', {
        value: () => undefined,
        configurable: true,
        writable: true,
      });
    }

    const createObjectUrlSpy = jest
      .spyOn(URL, 'createObjectURL')
      .mockReturnValue('blob:csv');
    const revokeObjectUrlSpy = jest
      .spyOn(URL, 'revokeObjectURL')
      .mockImplementation(() => undefined);

    const appendSpy = jest.spyOn(document.body, 'appendChild');
    const removeSpy = jest.spyOn(document.body, 'removeChild');
    const originalCreateElement = document.createElement.bind(document);
    const clickSpy = jest.fn();

    jest.spyOn(document, 'createElement').mockImplementation((tagName) => {
      const element = originalCreateElement(tagName);
      if (tagName === 'a') {
        (element as HTMLAnchorElement).click = clickSpy;
      }
      return element;
    });

    CSVHelper.downloadCSV('export.csv', 'a,b');

    expect(createObjectUrlSpy).toHaveBeenCalled();
    expect(appendSpy).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(removeSpy).toHaveBeenCalled();
    expect(revokeObjectUrlSpy).toHaveBeenCalledWith('blob:csv');
  });
});
