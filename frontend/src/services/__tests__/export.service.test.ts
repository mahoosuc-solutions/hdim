/**
 * Tests for export service
 * Following TDD approach - tests written first
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { exportToCSV, exportToJSON } from '../export.service';

describe('export.service', () => {
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

    // Mock URL.createObjectURL
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock-url') as unknown as typeof URL.createObjectURL;

    // Mock URL.revokeObjectURL
    revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('exportToCSV', () => {
    it('creates valid CSV format with headers', () => {
      const data = [
        { id: 1, name: 'John', age: 30 },
        { id: 2, name: 'Jane', age: 25 },
      ];

      exportToCSV(data, 'test-export');

      // Check that Blob was created with CSV content
      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;
      expect(blobCall).toBeInstanceOf(Blob);
      expect(blobCall.type).toBe('text/csv;charset=utf-8;');

      // Read blob content
      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        expect(content).toContain('id,name,age');
        expect(content).toContain('1,John,30');
        expect(content).toContain('2,Jane,25');
      };
      reader.readAsText(blobCall);
    });

    it('handles special characters (commas, quotes) in CSV', () => {
      const data = [
        { name: 'Smith, John', description: 'Says "hello"' },
        { name: 'O\'Brien', description: 'Multi\nline' },
      ];

      exportToCSV(data, 'test-special-chars');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        // Commas should be quoted
        expect(content).toContain('"Smith, John"');
        // Quotes should be escaped
        expect(content).toContain('Says ""hello""');
      };
      reader.readAsText(blobCall);
    });

    it('formats timestamps correctly as ISO strings', () => {
      const timestamp = new Date('2024-01-15T10:30:00.000Z');
      const data = [
        { id: 1, createdAt: timestamp, updatedAt: timestamp.getTime() },
      ];

      exportToCSV(data, 'test-timestamps');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        // Should contain ISO string format
        expect(content).toContain('2024-01-15T10:30:00.000Z');
      };
      reader.readAsText(blobCall);
    });

    it('flattens nested objects one level deep', () => {
      const data = [
        {
          id: 1,
          user: { name: 'John', email: 'john@example.com' },
          metadata: { created: '2024-01-01', updated: '2024-01-02' },
        },
      ];

      exportToCSV(data, 'test-nested');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        // Should flatten one level
        expect(content).toContain('user.name');
        expect(content).toContain('user.email');
        expect(content).toContain('metadata.created');
        expect(content).toContain('John');
        expect(content).toContain('john@example.com');
      };
      reader.readAsText(blobCall);
    });

    it('includes UTF-8 BOM for Excel compatibility', () => {
      const data = [{ name: 'Test' }];

      exportToCSV(data, 'test-bom');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        // UTF-8 BOM should be at the start
        expect(content.charCodeAt(0)).toBe(0xFEFF);
      };
      reader.readAsText(blobCall);
    });

    it('triggers browser download with correct filename', () => {
      const data = [{ id: 1, name: 'Test' }];

      exportToCSV(data, 'my-export');

      expect(mockLink.download).toBe('my-export.csv');
      expect(mockLink.href).toBe('blob:mock-url');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('revokes object URL after download', () => {
      const data = [{ id: 1 }];

      exportToCSV(data, 'test');

      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');
    });

    it('handles empty array', () => {
      const data: any[] = [];

      exportToCSV(data, 'empty-export');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        // Should only have BOM, no content
        expect(content.length).toBeLessThanOrEqual(1);
      };
      reader.readAsText(blobCall);
    });

    it('handles arrays in data by converting to string', () => {
      const data = [
        { id: 1, tags: ['javascript', 'typescript'] },
      ];

      exportToCSV(data, 'test-arrays');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        expect(content).toContain('javascript,typescript');
      };
      reader.readAsText(blobCall);
    });
  });

  describe('exportToJSON', () => {
    it('creates valid JSON format', () => {
      const data = [
        { id: 1, name: 'John', age: 30 },
        { id: 2, name: 'Jane', age: 25 },
      ];

      exportToJSON(data, 'test-export');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;
      expect(blobCall).toBeInstanceOf(Blob);
      expect(blobCall.type).toBe('application/json');

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        const parsed = JSON.parse(content);
        expect(parsed).toEqual(data);
      };
      reader.readAsText(blobCall);
    });

    it('formats JSON with proper indentation', () => {
      const data = [{ id: 1, name: 'Test' }];

      exportToJSON(data, 'test-formatted');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        // Should be pretty-printed with 2 space indentation
        expect(content).toContain('  ');
        expect(content).toContain('\n');
      };
      reader.readAsText(blobCall);
    });

    it('handles circular references gracefully', () => {
      const obj: any = { id: 1, name: 'Test' };
      obj.self = obj; // Create circular reference

      const data = [obj];

      // Should not throw error
      expect(() => {
        exportToJSON(data, 'test-circular');
      }).not.toThrow();

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        const parsed = JSON.parse(content);
        // Circular reference should be replaced with placeholder
        expect(parsed[0].self).toBe('[Circular]');
      };
      reader.readAsText(blobCall);
    });

    it('triggers browser download with correct filename', () => {
      const data = [{ id: 1, name: 'Test' }];

      exportToJSON(data, 'my-export');

      expect(mockLink.download).toBe('my-export.json');
      expect(mockLink.href).toBe('blob:mock-url');
      expect(mockLink.click).toHaveBeenCalled();
    });

    it('revokes object URL after download', () => {
      const data = [{ id: 1 }];

      exportToJSON(data, 'test');

      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');
    });

    it('handles empty array', () => {
      const data: any[] = [];

      exportToJSON(data, 'empty-export');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        const parsed = JSON.parse(content);
        expect(parsed).toEqual([]);
      };
      reader.readAsText(blobCall);
    });

    it('preserves Date objects as ISO strings', () => {
      const timestamp = new Date('2024-01-15T10:30:00.000Z');
      const data = [{ id: 1, createdAt: timestamp }];

      exportToJSON(data, 'test-dates');

      const blobCall = (globalThis.URL.createObjectURL as ReturnType<typeof vi.fn>).mock.calls[0][0] as Blob;

      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;
        expect(content).toContain('2024-01-15T10:30:00.000Z');
      };
      reader.readAsText(blobCall);
    });
  });
});
