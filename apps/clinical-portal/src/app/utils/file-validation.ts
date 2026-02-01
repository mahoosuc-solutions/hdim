/**
 * File Validation Utilities
 *
 * Provides client-side file validation for document uploads.
 * Validates file size and type before upload to ensure HIPAA compliance
 * and prevent unnecessary server requests.
 */

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB in bytes

const ACCEPTED_MIME_TYPES = [
  'application/pdf',
  'image/png',
  'image/jpeg',
  'image/tiff'
] as const;

/**
 * Validates file size against maximum allowed size
 *
 * @param file - The file to validate
 * @param maxSize - Maximum file size in bytes (default: 10 MB)
 * @returns true if file size is within limit, false otherwise
 *
 * @example
 * const file = new File(['test'], 'test.pdf', { type: 'application/pdf' });
 * validateFileSize(file); // Returns: true if <= 10 MB
 * validateFileSize(file, 5 * 1024 * 1024); // Returns: true if <= 5 MB
 */
export function validateFileSize(file: File, maxSize: number = MAX_FILE_SIZE): boolean {
  return file.size <= maxSize;
}

/**
 * Validates file type against accepted MIME types
 *
 * Accepted types: PDF, PNG, JPEG, TIFF
 *
 * @param file - The file to validate
 * @returns true if file type is accepted, false otherwise
 *
 * @example
 * const pdfFile = new File(['test'], 'test.pdf', { type: 'application/pdf' });
 * validateFileType(pdfFile); // Returns: true
 *
 * const txtFile = new File(['test'], 'test.txt', { type: 'text/plain' });
 * validateFileType(txtFile); // Returns: false
 */
export function validateFileType(file: File): boolean {
  return ACCEPTED_MIME_TYPES.includes(file.type as typeof ACCEPTED_MIME_TYPES[number]);
}

/**
 * Returns array of accepted MIME types
 *
 * Useful for setting the HTML5 file input accept attribute.
 *
 * @returns Array of accepted MIME type strings
 *
 * @example
 * const mimeTypes = getAcceptedMimeTypes();
 * // Returns: ['application/pdf', 'image/png', 'image/jpeg', 'image/tiff']
 *
 * const acceptAttribute = getAcceptedMimeTypes().join(',');
 * // Returns: 'application/pdf,image/png,image/jpeg,image/tiff'
 * // Use as: <input type="file" [attr.accept]="acceptAttribute" />
 */
export function getAcceptedMimeTypes(): string[] {
  return [...ACCEPTED_MIME_TYPES];
}

/**
 * Formats file size in human-readable format
 *
 * @param bytes - File size in bytes
 * @returns Formatted string (e.g., "1.5 MB")
 *
 * @example
 * formatFileSize(1024); // Returns: "1 KB"
 * formatFileSize(1536); // Returns: "1.5 KB"
 * formatFileSize(1048576); // Returns: "1 MB"
 * formatFileSize(10485760); // Returns: "10 MB"
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}
