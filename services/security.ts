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

export const sanitizeInput = (input: string): string => {
  if (!input) return '';
  // Trim whitespace and remove control characters (0-31 and 127), except tab (9) and newline (10, 13)
  // \x09 is tab, \x0A is newline, \x0D is carriage return
  // So we remove \x00-\x08, \x0B, \x0C, \x0E-\x1F, \x7F
  return input.trim().replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
};
