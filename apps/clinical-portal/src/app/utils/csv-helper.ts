/**
 * CSV Helper Utility
 *
 * Provides methods for properly escaping CSV values and generating CSV downloads.
 * Handles special characters (commas, quotes, newlines) according to RFC 4180.
 */
export class CSVHelper {
  /**
   * Escapes a value for CSV format according to RFC 4180
   * - Wraps in quotes if contains comma, quote, or newline
   * - Doubles internal quotes
   *
   * @param value - The value to escape
   * @returns Escaped string safe for CSV format
   *
   * @example
   * CSVHelper.escapeCSVValue('Smith, John') // Returns: "Smith, John"
   * CSVHelper.escapeCSVValue('Said "Hello"') // Returns: "Said ""Hello"""
   * CSVHelper.escapeCSVValue('Line1\nLine2') // Returns: "Line1\nLine2"
   */
  static escapeCSVValue(value: string | number | boolean | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }

    const strValue = String(value);

    // Check if value needs escaping (contains comma, quote, or newline)
    if (strValue.includes(',') || strValue.includes('"') || strValue.includes('\n') || strValue.includes('\r')) {
      // Double any quotes and wrap in quotes
      return `"${strValue.replace(/"/g, '""')}"`;
    }

    return strValue;
  }

  /**
   * Converts a 2D array of rows to a CSV string
   *
   * @param rows - Array of rows, where each row is an array of values
   * @returns CSV formatted string
   *
   * @example
   * const rows = [
   *   ['Name', 'Age', 'City'],
   *   ['Smith, John', 25, 'New York'],
   *   ['Doe, Jane', 30, 'Los Angeles']
   * ];
   * const csv = CSVHelper.arrayToCSV(rows);
   */
  static arrayToCSV(rows: (string | number | boolean | null | undefined)[][]): string {
    return rows.map(row =>
      row.map(value => this.escapeCSVValue(value)).join(',')
    ).join('\n');
  }

  /**
   * Triggers browser download of a CSV file
   *
   * @param filename - The filename for the download (should end in .csv)
   * @param content - The CSV content string
   *
   * @example
   * const csvContent = CSVHelper.arrayToCSV(rows);
   * CSVHelper.downloadCSV('export.csv', csvContent);
   */
  static downloadCSV(filename: string, content: string): void {
    // Add BOM for proper Excel UTF-8 handling
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + content], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // Clean up the URL object
    URL.revokeObjectURL(url);
  }

  /**
   * Formats a date for CSV export
   *
   * @param date - Date to format
   * @returns Formatted date string (YYYY-MM-DD HH:mm:ss)
   */
  static formatDate(date: Date | string | null | undefined): string {
    if (!date) {
      return '';
    }

    const dateObj = typeof date === 'string' ? new Date(date) : date;

    if (isNaN(dateObj.getTime())) {
      return '';
    }

    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    const hours = String(dateObj.getHours()).padStart(2, '0');
    const minutes = String(dateObj.getMinutes()).padStart(2, '0');
    const seconds = String(dateObj.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  /**
   * Formats a percentage for CSV export
   *
   * @param value - Numeric value (0-100 or 0-1)
   * @param isDecimal - Whether the value is already decimal (0-1) or percentage (0-100)
   * @returns Formatted percentage string
   */
  static formatPercentage(value: number | null | undefined, isDecimal = false): string {
    if (value === null || value === undefined || isNaN(value)) {
      return '';
    }

    const percentage = isDecimal ? value * 100 : value;
    return `${percentage.toFixed(1)}%`;
  }
}
