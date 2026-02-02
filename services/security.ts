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
  // Remove control characters (ASCII 0-31, 127) but keep \t (9), \n (10), \r (13)
  // \x00-\x08 matches ASCII 0-8
  // \x0B-\x0C matches ASCII 11-12 (Vertical Tab, Form Feed)
  // \x0E-\x1F matches ASCII 14-31
  // \x7F matches ASCII 127 (Delete)
  return input.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
};
