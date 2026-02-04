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

export const sanitizeInput = (input: string, maxLength: number = 1000): string => {
  if (!input) return '';
  // Remove control characters (0-31, 127) except tab, newline, carriage return
  let sanitized = input.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');

  if (sanitized.length > maxLength) {
    sanitized = sanitized.substring(0, maxLength);
  }
  return sanitized.trim();
};
