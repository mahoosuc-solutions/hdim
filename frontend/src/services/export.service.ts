/**
 * Export service for downloading data as CSV or JSON
 */

type ExportPrimitive = string | number | boolean | null | undefined | Date;
type ExportValue = ExportPrimitive | ExportValue[] | { [key: string]: ExportValue };
type ExportRecord = Record<string, ExportValue>;

/**
 * Flattens nested objects one level deep
 * { user: { name: 'John' } } becomes { 'user.name': 'John' }
 */
function flattenObject(obj: ExportRecord, prefix = ''): ExportRecord {
  const flattened: ExportRecord = {};

  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      const value = obj[key];
      const newKey = prefix ? `${prefix}.${key}` : key;

      if (value !== null && typeof value === 'object' && !(value instanceof Date) && !Array.isArray(value)) {
        // Flatten one level only
        Object.assign(flattened, flattenObject(value as ExportRecord, newKey));
      } else {
        flattened[newKey] = value;
      }
    }
  }

  return flattened;
}

/**
 * Converts value to CSV-safe string
 * Handles Date objects, escapes quotes, wraps values with commas/quotes
 */
function formatCSVValue(value: ExportValue): string {
  if (value === null || value === undefined) {
    return '';
  }

  if (value instanceof Date) {
    return value.toISOString();
  }

  if (typeof value === 'number' && !isNaN(value)) {
    return String(value);
  }

  if (Array.isArray(value)) {
    return value.join(',');
  }

  let stringValue = String(value);

  // Escape double quotes by doubling them
  if (stringValue.includes('"')) {
    stringValue = stringValue.replace(/"/g, '""');
  }

  // Wrap in quotes if contains comma, newline, or quotes
  if (stringValue.includes(',') || stringValue.includes('\n') || stringValue.includes('"')) {
    return `"${stringValue}"`;
  }

  return stringValue;
}

/**
 * Export data to CSV format and trigger download
 */
export function exportToCSV(data: ExportRecord[], filename: string): void {
  if (data.length === 0) {
    // Create empty blob with just BOM
    const blob = new Blob(['\uFEFF'], { type: 'text/csv;charset=utf-8;' });
    triggerDownload(blob, `${filename}.csv`);
    return;
  }

  // Flatten all objects
  const flatData = data.map(item => flattenObject(item));

  // Get all unique headers
  const headers = Array.from(
    new Set(flatData.flatMap(item => Object.keys(item)))
  );

  // Create CSV content
  const csvRows: string[] = [];

  // Add header row
  csvRows.push(headers.join(','));

  // Add data rows
  for (const item of flatData) {
    const values = headers.map(header => {
      const value = item[header];
      return formatCSVValue(value);
    });
    csvRows.push(values.join(','));
  }

  // Join with newlines and add UTF-8 BOM for Excel compatibility
  const csvContent = '\uFEFF' + csvRows.join('\n');

  // Create blob and trigger download
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  triggerDownload(blob, `${filename}.csv`);
}

/**
 * Handles circular references in objects
 */
function getCircularReplacer() {
  const seen = new WeakSet();
  return (_key: string, value: unknown) => {
    if (typeof value === 'object' && value !== null) {
      if (seen.has(value)) {
        return '[Circular]';
      }
      seen.add(value);
    }
    return value;
  };
}

/**
 * Export data to JSON format and trigger download
 */
export function exportToJSON(data: ExportRecord[], filename: string): void {
  // Convert to JSON with pretty printing and circular reference handling
  const jsonContent = JSON.stringify(data, getCircularReplacer(), 2);

  // Create blob and trigger download
  const blob = new Blob([jsonContent], { type: 'application/json' });
  triggerDownload(blob, `${filename}.json`);
}

/**
 * Triggers browser download for a blob
 */
function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();

  // Clean up
  URL.revokeObjectURL(url);
}
