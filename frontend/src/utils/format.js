// Utility formatting functions for WMS

export const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return '';
  
  return date.toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

// Local calendar date as YYYY-MM-DD, unlike `Date#toISOString()` which is UTC and
// reads as the previous day for the first several hours of every local day in any
// timezone ahead of UTC (e.g. UTC+7 Vietnam, midnight-7am).
export const getLocalDateString = (date = new Date()) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getAvatarFallback = (fullName) => {
  if (!fullName) return '?';
  const parts = fullName.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  
  const last = parts[parts.length - 1];
  const first = parts[0];
  return (first[0] + last[0]).toUpperCase();
};

export const formatNumber = (num) => {
  if (num === null || num === undefined) return '0';
  return Number(num).toLocaleString('vi-VN');
};
