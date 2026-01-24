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
