/**
 * Excel Export Service
 * Provides functionality to export data to Excel (.xlsx) format with formatting
 * Implemented using Test-Driven Development (TDD)
 */

import * as XLSX from 'xlsx';

export interface ExcelExportOptions {
  filename?: string;
  sheetName?: string;
  includeHeaders?: boolean;
  columnWidths?: number[];
  formatting?: {
    headerStyle?: 'bold' | 'normal';
    dateFormat?: string;
    numberFormat?: string;
  };
}

export interface MultiSheetData {
  sheetName: string;
  data: any[];
  options?: ExcelExportOptions;
}

/**
 * Converts camelCase string to Title Case
 * Example: "successRate" -> "Success Rate"
 */
function camelToTitleCase(str: string): string {
  // Insert space before uppercase letters and capitalize first letter
  const result = str
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (char) => char.toUpperCase());
  return result.trim();
}

/**
 * Preprocesses data for Excel export
 * - Converts camelCase headers to Title Case
 * - Handles special data types (dates, booleans, nested objects, arrays)
 * - Converts null/undefined to empty strings
 */
function preprocessData(data: any[], includeHeaders = true): any[] {
  if (!data || data.length === 0) {
    return [];
  }

  return data.map((row) => {
    const processedRow: any = {};

    Object.keys(row).forEach((key) => {
      // Convert key to Title Case if headers are included
      const displayKey = includeHeaders ? camelToTitleCase(key) : key;
      const value = row[key];

      // Handle different data types
      if (value === null || value === undefined) {
        processedRow[displayKey] = '';
      } else if (typeof value === 'boolean') {
        processedRow[displayKey] = value ? 'Yes' : 'No';
      } else if (value instanceof Date) {
        // Keep as Date object - xlsx will handle formatting
        processedRow[displayKey] = value;
      } else if (Array.isArray(value)) {
        // Convert arrays to comma-separated string
        processedRow[displayKey] = value.join(', ');
      } else if (typeof value === 'object') {
        // Convert nested objects to JSON string
        processedRow[displayKey] = JSON.stringify(value);
      } else {
        processedRow[displayKey] = value;
      }
    });

    return processedRow;
  });
}

/**
 * Applies column widths to worksheet
 */
function applyColumnWidths(
  worksheet: XLSX.WorkSheet,
  columnWidths?: number[]
): void {
  if (!columnWidths || columnWidths.length === 0) {
    return;
  }

  worksheet['!cols'] = columnWidths.map((width) => ({ wch: width }));
}

/**
 * Creates an Excel Blob from data array
 * @param data Array of objects to export
 * @param options Export options
 * @returns Blob containing Excel file data
 */
export function createExcelBlob(
  data: any[],
  options: ExcelExportOptions = {}
): Blob {
  const {
    sheetName = 'Sheet1',
    includeHeaders = true,
    columnWidths,
  } = options;

  // Preprocess data
  const processedData = preprocessData(data, includeHeaders);

  // Create worksheet from data
  const worksheet = XLSX.utils.json_to_sheet(processedData, {
    skipHeader: !includeHeaders,
  });

  // Apply column widths if specified
  applyColumnWidths(worksheet, columnWidths);

  // Create workbook and add worksheet
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, sheetName);

  // Generate Excel file as array buffer
  const excelBuffer = XLSX.write(workbook, {
    bookType: 'xlsx',
    type: 'array',
  });

  // Create blob with correct MIME type
  return new Blob([excelBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
}

/**
 * Exports data to Excel file and triggers download
 * @param data Array of objects to export
 * @param options Export options including filename
 */
export async function exportToExcel(
  data: any[],
  options: ExcelExportOptions = {}
): Promise<void> {
  const { filename = 'export' } = options;

  // Create Excel blob
  const blob = createExcelBlob(data, options);

  // Ensure filename has .xlsx extension
  const finalFilename = filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`;

  // Create download link
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = finalFilename;

  // Trigger download
  document.body.appendChild(link);
  link.click();

  // Cleanup
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Exports multiple sheets to a single Excel workbook
 * @param sheets Array of sheet data with names and options
 * @param filename Filename for the workbook
 */
export async function exportMultiSheet(
  sheets: MultiSheetData[],
  filename = 'workbook'
): Promise<void> {
  // Create new workbook
  const workbook = XLSX.utils.book_new();

  // Add each sheet to workbook
  sheets.forEach((sheet) => {
    const { sheetName, data, options = {} } = sheet;
    const { includeHeaders = true, columnWidths } = options;

    // Preprocess data
    const processedData = preprocessData(data, includeHeaders);

    // Create worksheet
    const worksheet = XLSX.utils.json_to_sheet(processedData, {
      skipHeader: !includeHeaders,
    });

    // Apply column widths if specified
    applyColumnWidths(worksheet, columnWidths);

    // Add worksheet to workbook
    XLSX.utils.book_append_sheet(workbook, worksheet, sheetName);
  });

  // Generate Excel file as array buffer
  const excelBuffer = XLSX.write(workbook, {
    bookType: 'xlsx',
    type: 'array',
  });

  // Create blob
  const blob = new Blob([excelBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });

  // Ensure filename has .xlsx extension
  const finalFilename = filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`;

  // Create download link
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = finalFilename;

  // Trigger download
  document.body.appendChild(link);
  link.click();

  // Cleanup
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
