export const isValidUrl = (url: string): boolean => {
  if (!url) return false;
  // Allowed protocols for app links
  const allowedProtocols = ['http:', 'https:', 'market:', 'itms-apps:', 'itms-services:'];

  try {
    const parsed = new URL(url);
    return allowedProtocols.includes(parsed.protocol.toLowerCase());
  } catch (e) {
    // URL parsing failed (e.g. relative URL or invalid format)
    return false;
  }
};

export const sanitizeUrl = (url: string): string => {
  return isValidUrl(url) ? url : '';
};

/**
 * Sanitizes input by removing non-printable control characters.
 * Preserves tabs (\t), newlines (\n), and carriage returns (\r).
 * This helps prevent log forging and some injection attacks.
 */
export const sanitizeInput = (input: string): string => {
  if (!input) return '';
  // Remove non-printable control characters (ASCII 0-31), except for:
  // \t (9), \n (10), \r (13)
  // Also remove delete (127)
  return input.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
};
