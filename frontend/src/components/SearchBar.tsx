/**
 * SearchBar component
 * Search/filter input with debouncing and keyboard shortcuts
 */

import { useState, useEffect, useRef } from 'react';
import {
  TextField,
  InputAdornment,
  IconButton,
  CircularProgress,
} from '@mui/material';
import { Search as SearchIcon, Clear as ClearIcon } from '@mui/icons-material';
import { useDebounce } from '../hooks/useDebounce';

export interface SearchBarProps {
  /** Callback function called with the search query */
  onSearch: (query: string) => void;
  /** Placeholder text for the input */
  placeholder?: string;
  /** Debounce delay in milliseconds (default: 300ms) */
  debounceMs?: number;
  /** Whether to show loading indicator */
  isLoading?: boolean;
  /** Whether to auto-focus on mount */
  autoFocus?: boolean;
}

/**
 * SearchBar component with debouncing and keyboard shortcuts
 */
export function SearchBar({
  onSearch,
  placeholder = 'Search...',
  debounceMs = 300,
  isLoading = false,
  autoFocus = false,
}: SearchBarProps) {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, debounceMs);
  const inputRef = useRef<HTMLInputElement>(null);
  const isFirstRender = useRef(true);

  // Call onSearch when debounced query changes (skip initial render)
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    onSearch(debouncedQuery);
  }, [debouncedQuery, onSearch]);

  // Handle keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Check for Ctrl+K or Cmd+K
      if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
        event.preventDefault();
        inputRef.current?.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const handleClear = () => {
    setQuery('');
    // Call onSearch immediately with empty string (no debounce)
    onSearch('');
  };

  return (
    <TextField
      inputRef={inputRef}
      value={query}
      onChange={(e) => setQuery(e.target.value)}
      placeholder={placeholder}
      autoFocus={autoFocus}
      fullWidth
      size="small"
      inputProps={{
        'aria-label': 'Search',
      }}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            {isLoading ? (
              <CircularProgress size={20} />
            ) : (
              <SearchIcon data-testid="SearchIcon" />
            )}
          </InputAdornment>
        ),
        endAdornment: query && (
          <InputAdornment position="end">
            <IconButton
              onClick={handleClear}
              edge="end"
              size="small"
              aria-label="Clear search"
            >
              <ClearIcon />
            </IconButton>
          </InputAdornment>
        ),
      }}
    />
  );
}
