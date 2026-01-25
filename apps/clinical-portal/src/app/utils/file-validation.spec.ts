import { validateFileSize, validateFileType, getAcceptedMimeTypes, formatFileSize } from './file-validation';

describe('File Validation Utilities', () => {
  describe('validateFileSize', () => {
    it('should return true for files <= 10 MB', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 10 * 1024 * 1024 }); // 10 MB
      expect(validateFileSize(file)).toBe(true);
    });

    it('should return false for files > 10 MB', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 }); // 11 MB
      expect(validateFileSize(file)).toBe(false);
    });

    it('should handle custom max size', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      Object.defineProperty(file, 'size', { value: 5 * 1024 * 1024 }); // 5 MB
      expect(validateFileSize(file, 6 * 1024 * 1024)).toBe(true);
      expect(validateFileSize(file, 4 * 1024 * 1024)).toBe(false);
    });
  });

  describe('validateFileType', () => {
    it('should return true for PDF files', () => {
      const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
      expect(validateFileType(file)).toBe(true);
    });

    it('should return true for PNG files', () => {
      const file = new File(['test'], 'test.png', { type: 'image/png' });
      expect(validateFileType(file)).toBe(true);
    });

    it('should return true for JPG/JPEG files', () => {
      const file1 = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const file2 = new File(['test'], 'test.jpeg', { type: 'image/jpeg' });
      expect(validateFileType(file1)).toBe(true);
      expect(validateFileType(file2)).toBe(true);
    });

    it('should return true for TIFF files', () => {
      const file = new File(['test'], 'test.tiff', { type: 'image/tiff' });
      expect(validateFileType(file)).toBe(true);
    });

    it('should return false for unsupported file types', () => {
      const file = new File(['test'], 'test.txt', { type: 'text/plain' });
      expect(validateFileType(file)).toBe(false);
    });
  });

  describe('getAcceptedMimeTypes', () => {
    it('should return array of accepted MIME types', () => {
      const mimeTypes = getAcceptedMimeTypes();
      expect(mimeTypes).toContain('application/pdf');
      expect(mimeTypes).toContain('image/png');
      expect(mimeTypes).toContain('image/jpeg');
      expect(mimeTypes).toContain('image/tiff');
      expect(mimeTypes.length).toBe(4);
    });

    it('should return string for input accept attribute', () => {
      const acceptString = getAcceptedMimeTypes().join(',');
      expect(acceptString).toBe('application/pdf,image/png,image/jpeg,image/tiff');
    });
  });

  describe('formatFileSize', () => {
    it('should format zero bytes', () => {
      expect(formatFileSize(0)).toBe('0 Bytes');
    });

    it('should format bytes', () => {
      expect(formatFileSize(500)).toBe('500 Bytes');
    });

    it('should format kilobytes', () => {
      expect(formatFileSize(1024)).toBe('1 KB');
      expect(formatFileSize(1536)).toBe('1.5 KB');
    });

    it('should format megabytes', () => {
      expect(formatFileSize(1048576)).toBe('1 MB');
      expect(formatFileSize(10485760)).toBe('10 MB');
    });

    it('should format gigabytes', () => {
      expect(formatFileSize(1073741824)).toBe('1 GB');
    });
  });
});
