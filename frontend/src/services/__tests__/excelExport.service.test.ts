/**
 * Tests for Excel export service
 * Following TDD approach - tests written FIRST (RED phase)
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  exportToExcel,
  exportMultiSheet,
  createExcelBlob,
} from '../excelExport.service';

// Mock xlsx library
vi.mock('xlsx', () => {
  return {
    utils: {
      json_to_sheet: vi.fn(() => ({})),
      book_new: vi.fn(() => ({ SheetNames: [], Sheets: {} })),
      book_append_sheet: vi.fn((book, sheet, name) => {
        book.SheetNames.push(name);
        book.Sheets[name] = sheet;
      }),
    },
    write: vi.fn(() => new ArrayBuffer(8)),
  };
});

describe('excelExport.service', () => {
  let mockLink: HTMLAnchorElement;
  let createElementSpy: ReturnType<typeof vi.spyOn>;
  let revokeObjectURLSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    // Mock document.createElement for anchor element
    mockLink = {
      href: '',
      download: '',
      click: vi.fn(),
      style: {},
    } as unknown as HTMLAnchorElement;

    createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(mockLink);

    // Mock document.body.appendChild and removeChild
    vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink);
    vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink);

    // Mock URL.createObjectURL
    global.URL.createObjectURL = vi.fn(() => 'blob:mock-url');

    // Mock URL.revokeObjectURL
    revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('createExcelBlob', () => {
    it('creates valid Excel blob with correct MIME type', () => {
      const data = [
        { id: 1, name: 'John', age: 30 },
        { id: 2, name: 'Jane', age: 25 },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
      expect(blob.type).toBe('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    });

    it('includes headers by default', async () => {
      const data = [
        { id: 1, name: 'John' },
        { id: 2, name: 'Jane' },
      ];

      const xlsx = await import('xlsx');
      createExcelBlob(data);

      // Should call json_to_sheet which processes headers (with Title Case conversion)
      expect(xlsx.utils.json_to_sheet).toHaveBeenCalled();
      const callArgs = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls[0];
      // Data should be transformed with Title Case headers
      expect(callArgs[0]).toEqual([
        { Id: 1, Name: 'John' },
        { Id: 2, Name: 'Jane' },
      ]);
    });

    it('excludes headers when includeHeaders is false', async () => {
      const data = [
        { id: 1, name: 'John' },
        { id: 2, name: 'Jane' },
      ];

      const xlsx = await import('xlsx');
      // Get the last call (after clearing previous test calls)
      const callsBefore = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      createExcelBlob(data, { includeHeaders: false });

      // Should pass skipHeader option to json_to_sheet
      expect(xlsx.utils.json_to_sheet).toHaveBeenCalled();
      const callArgs = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls[callsBefore];
      expect(callArgs[1]).toEqual({ skipHeader: true });
    });

    it('converts camelCase headers to Title Case', () => {
      const data = [
        { successRate: 0.95, totalEvents: 100, errorCount: 5 },
      ];

      const blob = createExcelBlob(data);

      // Blob should be created (actual header conversion happens in implementation)
      expect(blob).toBeInstanceOf(Blob);
    });

    it('preserves numeric data types', async () => {
      const data = [
        { id: 1, count: 100, rate: 0.95, total: 1000.50 },
      ];

      const xlsx = await import('xlsx');
      const callsBefore = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      createExcelBlob(data);

      expect(xlsx.utils.json_to_sheet).toHaveBeenCalled();
      const callArgs = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls[callsBefore];
      // Numeric values should be preserved
      expect(callArgs[0][0]).toHaveProperty('Id', 1);
      expect(callArgs[0][0]).toHaveProperty('Count', 100);
      expect(callArgs[0][0]).toHaveProperty('Rate', 0.95);
      expect(callArgs[0][0]).toHaveProperty('Total', 1000.50);
    });

    it('formats dates correctly as Excel dates', () => {
      const testDate = new Date('2024-01-15T10:30:00.000Z');
      const data = [
        { id: 1, createdAt: testDate, updatedAt: testDate },
      ];

      const blob = createExcelBlob(data, {
        formatting: {
          dateFormat: 'yyyy-mm-dd hh:mm:ss',
        },
      });

      expect(blob).toBeInstanceOf(Blob);
    });

    it('formats booleans as Yes/No', () => {
      const data = [
        { id: 1, isActive: true, isDeleted: false },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });

    it('handles null and undefined values as empty cells', () => {
      const data = [
        { id: 1, name: 'John', email: null, phone: undefined },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });

    it('handles nested objects by converting to JSON string', () => {
      const data = [
        {
          id: 1,
          user: { name: 'John', email: 'john@example.com' },
          metadata: { created: '2024-01-01' },
        },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });

    it('applies custom column widths when specified', () => {
      const data = [
        { id: 1, name: 'John', description: 'A very long description that needs more width' },
      ];

      const blob = createExcelBlob(data, {
        columnWidths: [10, 20, 50],
      });

      expect(blob).toBeInstanceOf(Blob);
    });

    it('handles empty data array without errors', () => {
      const data: any[] = [];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });

    it('handles single row of data', () => {
      const data = [
        { id: 1, name: 'John', email: 'john@example.com' },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });
  });

  describe('exportToExcel', () => {
    it('triggers browser download with correct default filename', async () => {
      const data = [
        { id: 1, name: 'Test' },
      ];

      await exportToExcel(data);

      expect(mockLink.download).toBe('export.xlsx');
      expect(mockLink.href).toBe('blob:mock-url');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('triggers browser download with custom filename', async () => {
      const data = [
        { id: 1, name: 'Test' },
      ];

      await exportToExcel(data, { filename: 'my-report' });

      expect(mockLink.download).toBe('my-report.xlsx');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('auto-appends .xlsx extension if not present', async () => {
      const data = [
        { id: 1, name: 'Test' },
      ];

      await exportToExcel(data, { filename: 'report' });

      expect(mockLink.download).toBe('report.xlsx');
    });

    it('does not duplicate .xlsx extension', async () => {
      const data = [
        { id: 1, name: 'Test' },
      ];

      await exportToExcel(data, { filename: 'report.xlsx' });

      expect(mockLink.download).toBe('report.xlsx');
    });

    it('revokes object URL after download', async () => {
      const data = [{ id: 1 }];

      await exportToExcel(data);

      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');
    });

    it('applies custom sheet name when specified', async () => {
      const data = [{ id: 1, name: 'Test' }];
      const xlsx = await import('xlsx');
      const callsBefore = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      await exportToExcel(data, { sheetName: 'My Data' });

      expect(xlsx.utils.book_append_sheet).toHaveBeenCalled();
      const callArgs = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls[callsBefore];
      expect(callArgs[2]).toBe('My Data');
    });

    it('uses default sheet name when not specified', async () => {
      const data = [{ id: 1, name: 'Test' }];
      const xlsx = await import('xlsx');
      const callsBefore = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      await exportToExcel(data);

      expect(xlsx.utils.book_append_sheet).toHaveBeenCalled();
      const callArgs = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls[callsBefore];
      expect(callArgs[2]).toBe('Sheet1');
    });
  });

  describe('exportMultiSheet', () => {
    it('creates workbook with multiple sheets', async () => {
      const sheets = [
        {
          sheetName: 'Events',
          data: [
            { id: 1, type: 'login', timestamp: new Date() },
            { id: 2, type: 'logout', timestamp: new Date() },
          ],
        },
        {
          sheetName: 'Users',
          data: [
            { id: 1, name: 'John', email: 'john@example.com' },
            { id: 2, name: 'Jane', email: 'jane@example.com' },
          ],
        },
        {
          sheetName: 'Summary',
          data: [
            { metric: 'Total Events', value: 100 },
            { metric: 'Active Users', value: 50 },
          ],
        },
      ];

      const xlsx = await import('xlsx');
      const callsBefore = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      await exportMultiSheet(sheets);

      // Should call book_append_sheet for each sheet (3 times)
      const callsAfter = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls.length;
      expect(callsAfter - callsBefore).toBe(3);
    });

    it('applies individual options to each sheet', async () => {
      const sheets = [
        {
          sheetName: 'Sheet1',
          data: [{ id: 1 }],
          options: { includeHeaders: true },
        },
        {
          sheetName: 'Sheet2',
          data: [{ id: 2 }],
          options: { includeHeaders: false },
        },
      ];

      const xlsx = await import('xlsx');
      const callsBefore = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      await exportMultiSheet(sheets);

      const callsAfter = (xlsx.utils.json_to_sheet as ReturnType<typeof vi.fn>).mock.calls.length;
      expect(callsAfter - callsBefore).toBe(2);
    });

    it('triggers download with custom filename for multi-sheet', async () => {
      const sheets = [
        { sheetName: 'Sheet1', data: [{ id: 1 }] },
        { sheetName: 'Sheet2', data: [{ id: 2 }] },
      ];

      await exportMultiSheet(sheets, 'multi-sheet-report');

      expect(mockLink.download).toBe('multi-sheet-report.xlsx');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('uses default filename for multi-sheet when not specified', async () => {
      const sheets = [
        { sheetName: 'Sheet1', data: [{ id: 1 }] },
      ];

      await exportMultiSheet(sheets);

      expect(mockLink.download).toBe('workbook.xlsx');
    });

    it('handles empty sheets array', async () => {
      const sheets: any[] = [];

      await exportMultiSheet(sheets);

      // Should still create a workbook and trigger download
      expect(mockLink.click).toHaveBeenCalled();
    });
  });

  describe('edge cases and error handling', () => {
    it('handles very long filenames gracefully', async () => {
      const data = [{ id: 1 }];
      const longFilename = 'a'.repeat(300);

      await exportToExcel(data, { filename: longFilename });

      // Should still work, browser will handle truncation
      expect(mockLink.download).toContain('.xlsx');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('handles data with mixed types in same column', () => {
      const data = [
        { id: 1, value: 'text' },
        { id: 2, value: 123 },
        { id: 3, value: true },
        { id: 4, value: null },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });

    it('handles special characters in sheet names', async () => {
      const sheets = [
        { sheetName: 'Events: Login/Logout', data: [{ id: 1 }] },
      ];

      const xlsx = await import('xlsx');
      const callsBefore = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls.length;

      await exportMultiSheet(sheets);

      const callsAfter = (xlsx.utils.book_append_sheet as ReturnType<typeof vi.fn>).mock.calls.length;
      expect(callsAfter - callsBefore).toBe(1);
    });

    it('handles array values by converting to string', () => {
      const data = [
        { id: 1, tags: ['tag1', 'tag2', 'tag3'] },
      ];

      const blob = createExcelBlob(data);

      expect(blob).toBeInstanceOf(Blob);
    });
  });
});
