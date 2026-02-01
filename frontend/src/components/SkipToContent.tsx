/**
 * Skip to Content Link for Keyboard Navigation
 *
 * WCAG 2.4.1 (Level A) - Bypass Blocks
 *
 * Provides keyboard users a way to bypass navigation and jump directly
 * to main content. The link is visually hidden until focused via keyboard,
 * ensuring it doesn't interfere with the visual design while remaining
 * accessible to screen readers and keyboard users.
 *
 * Usage:
 * 1. Add <SkipToContent /> as first element in App
 * 2. Add id="main-content" to main content container
 * 3. Press Tab on page load to reveal and activate
 */

import { styled } from '@mui/material/styles';
import { Link } from '@mui/material';

const StyledSkipLink = styled(Link)(({ theme }) => ({
  position: 'absolute',
  left: '-9999px',
  top: 'auto',
  width: '1px',
  height: '1px',
  overflow: 'hidden',
  zIndex: theme.zIndex.tooltip + 1,

  '&:focus': {
    position: 'fixed',
    top: theme.spacing(2),
    left: theme.spacing(2),
    width: 'auto',
    height: 'auto',
    padding: theme.spacing(2, 3),
    background: theme.palette.primary.main,
    color: theme.palette.primary.contrastText,
    textDecoration: 'none',
    borderRadius: theme.shape.borderRadius,
    boxShadow: theme.shadows[8],
    fontSize: '1rem',
    fontWeight: 600,
    outline: `3px solid ${theme.palette.primary.light}`,
    outlineOffset: '2px',
    zIndex: theme.zIndex.tooltip + 1,
  },
}));

export function SkipToContent() {
  const handleClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    const mainContent = document.getElementById('main-content');
    if (mainContent) {
      mainContent.focus();
      mainContent.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <StyledSkipLink
      href="#main-content"
      onClick={handleClick}
      tabIndex={0}
    >
      Skip to main content
    </StyledSkipLink>
  );
}
