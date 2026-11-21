export function countWords(text: string): number {
  return text
    .trim()
    .split(/\s+/u)
    .filter((word) => word.length > 0).length;
}

export function truncate(text: string, maxChars: number): string {
  if (text.length <= maxChars) {
    return text;
  }
  return `${text.slice(0, maxChars - 1)}…`;
}
