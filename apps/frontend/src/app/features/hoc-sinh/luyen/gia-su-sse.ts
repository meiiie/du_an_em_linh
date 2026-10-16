import { SuKienSse } from '../../../api/gia-su';

/** Chỉ lấy frame hoàn chỉnh. Giữ nguyên CR cuối chunk để không cắt đôi CRLF. */
export function docSse(raw: string, eof = false): { events: SuKienSse[]; rest: string } {
  const events: SuKienSse[] = [];
  const lines: string[] = [];
  const ends = /\r\n|\r|\n/g;
  let start = 0, committed = 0;
  for (let match = ends.exec(raw); match; match = ends.exec(raw)) {
    if (!eof && match[0] === '\r' && match.index === raw.length - 1) break;
    const line = raw.slice(start, match.index);
    start = match.index + match[0].length;
    if (line) { lines.push(line); continue; }
    let event = 'message', id: string | undefined;
    const data: string[] = [];
    for (const field of lines) {
      const colon = field.indexOf(':');
      const name = colon < 0 ? field : field.slice(0, colon);
      const value = colon < 0 ? '' : field.slice(colon + 1).replace(/^ /, '');
      if (name === 'event') event = value;
      if (name === 'id' && !value.includes('\0')) id = value;
      if (name === 'data') data.push(value);
    }
    if (data.length) {
      try {
        if (['start', 'status', 'delta', 'done', 'error', 'heartbeat'].includes(event)) {
          events.push({ event, id, data: JSON.parse(data.join('\n')) } as SuKienSse);
        }
      }
      catch { throw new Error('Kết nối bị gián đoạn. Bấm Kết nối lại để tiếp tục.'); }
    }
    lines.length = 0;
    committed = start;
  }
  return { events, rest: raw.slice(committed) };
}
