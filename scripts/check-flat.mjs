import { readdir, readFile } from 'node:fs/promises';
import { resolve, relative, extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const blank = value => value.replace(/[^\n]/g, ' ');

// Keep offsets/newlines intact; quoted URLs and comment-like CSS content survive.
function withoutComments(source, html = false) {
  let result = '';
  for (let i = 0; i < source.length;) {
    if (html && source.startsWith('<!--', i)) {
      const end = source.indexOf('-->', i + 4);
      const stop = end < 0 ? source.length : end + 3;
      result += blank(source.slice(i, stop)); i = stop;
    } else if (source.startsWith('/*', i) || (!html && source.startsWith('//', i))) {
      const block = source[i + 1] === '*';
      const end = source.indexOf(block ? '*/' : '\n', i + 2);
      const stop = end < 0 ? source.length : end + (block ? 2 : 0);
      result += blank(source.slice(i, stop)); i = stop;
    } else if ('\"\'`'.includes(source[i])) {
      const quote = source[i], start = i++;
      while (i < source.length) {
        if (source[i] === '\\') i += 2;
        else if (source[i++] === quote) break;
      }
      result += source.slice(start, i);
    } else result += source[i++];
  }
  return result;
}

function literals(source) {
  const tokens = [];
  const pattern = /(['"`])(?:\\[\s\S]|(?!\1)[^\\])*?\1|[\w$-]+|[^\s]/g;
  for (const match of source.matchAll(pattern)) {
    tokens.push({ value: match[0], start: match.index, string: !!match[1] });
  }
  return tokens;
}

/** Static policy check, not a browser CSS evaluator. Indices always refer to original source. */
export function scanSource(source, path = 'source.css') {
  const findings = [];
  const report = (rule, offset, message) => findings.push({
    path, line: source.slice(0, offset).split('\n').length, rule, message, offset,
  });
  const utility = (text, offset) => {
    const pattern = /(?<![\w-])(?:inset-)?shadow(?:-[\w-]+|\[[^\]]+\])?(?![\w-])/g;
    for (const match of text.matchAll(pattern)) {
      if (match[0] === 'shadow-none' || match[0] === 'inset-shadow-none') continue;
      report('shadow-utility', offset + match.index, `Utility bóng bị cấm: ${match[0]}`);
    }
  };
  const css = (text, offset) => {
    const clean = withoutComments(text).replace(/(['"])(?:\\[\s\S]|(?!\1)[^\\])*?\1/g, blank);
    const declaration = /(?:^|[;{])\s*([-\w]+)\s*:\s*([^;{}]+)/g;
    for (const match of clean.matchAll(declaration)) {
      const property = match[1].toLowerCase();
      const value = match[2].replace(/!important\s*$/i, '').trim();
      const start = offset + match.index + match[0].indexOf(match[1]);
      if (/^(?:-webkit-|-moz-)?box-shadow$/.test(property) && !/^none$/i.test(value)) {
        report('shadow', start, 'box-shadow phải là none (kể cả inset).');
      }
      if (/^border-(?:left|inline-start)(?:-(?:color|style|width))?$/.test(property)
          && !/^(?:none|0(?:px|rem|em)?|transparent)$/i.test(value)) {
        report('edge-stripe', start, 'Không dùng viền bên trái/inline-start; dùng viền đều hoặc nền.');
      }
    }
    for (const match of clean.matchAll(/--shadow-[\w-]+/g)) {
      report('shadow-token', offset + match.index, `Token bóng bị cấm: ${match[0]}`);
    }
    for (const match of clean.matchAll(/@apply\s+([^;{}]+)/g)) utility(match[1], offset + match.index + match[0].indexOf(match[1]));
    // Catch the usual pseudo-element replacement for a vertical edge stripe.
    for (const match of clean.matchAll(/([^{}]+)\{([^{}]*)\}/g)) {
      for (const token of match[1].matchAll(/\.((?:[\w-]+\\:)*(?:inset-)?shadow(?:-[\w-]+)?)/g)) {
        utility(token[1].replace(/\\:/g, ':'), offset + match.index + token.index + 1);
      }
      const body = match[2];
      if (/::?(?:before|after)/.test(match[1])
          && /(?:left|right|inset-inline-start|inset-inline-end)\s*:\s*0(?:px)?\b/.test(body)
          && /width\s*:\s*(?:[1-4](?:px)?|0\.(?:125|25)rem)\b/.test(body)
          && /background(?:-color)?\s*:\s*(?!none\b|transparent\b)[^;]+/.test(body)) {
        report('edge-stripe', offset + match.index, 'Pseudo-element tạo dải màu mép bị cấm.');
      }
    }
  };
  const html = (text, offset) => {
    const clean = withoutComments(text, true);
    for (const tag of clean.matchAll(/<[^>]+>/g)) {
      for (const attr of tag[0].matchAll(/([^\s=<>]+)\s*=\s*(["'])([\s\S]*?)\2/g)) {
        const name = attr[1].toLowerCase(), value = attr[3];
        const start = offset + tag.index + attr.index;
        const contentStart = start + attr[0].indexOf(attr[2]) + 1;
        if (name === 'style') css(value, contentStart);
        if (name === 'class' || name === '[ngclass]' || name === '[class]' || name.startsWith('[class.')) {
          utility(name.startsWith('[class.') ? name.slice(7, -1) : value, name.startsWith('[class.') ? start + 7 : contentStart);
        }
        if (/^\[style\.(?:box-shadow|boxshadow)\]$/.test(name) && !/^(?:['"]none['"]|null)$/.test(value.trim())) {
          report('shadow', start, 'Style bóng động bị cấm; dùng outline cho focus.');
        }
        if (/^\[style\.border(?:-left|-inline-start|left|inlinestart)/.test(name)) {
          report('edge-stripe', start, 'Style viền trái động bị cấm.');
        }
        if (name === '[ngstyle]' || name === '[style]') {
          for (const property of value.matchAll(/(?:['"]?)(box-shadow|boxShadow|border-left|borderLeft|border-inline-start|borderInlineStart)(?:['"]?)\s*:\s*([^,}]+)/g)) {
            const reset = /^(?:['"]none['"]|null|0)$/.test(property[2].trim());
            if (!reset) report(property[1].startsWith('box') ? 'shadow' : 'edge-stripe', contentStart + property.index, 'Style object tạo bóng hoặc viền nhấn trái bị cấm.');
          }
        }
      }
    }
    for (const match of clean.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style\s*>/gi)) {
      css(match[1], offset + match.index + match[0].indexOf('>') + 1);
    }
  };
  const extension = extname(path).toLowerCase();
  if (['.css', '.scss', '.sass', '.less'].includes(extension)) css(source, 0);
  else if (extension === '.html') html(source, 0);
  else if (extension === '.ts') {
    const tokens = literals(withoutComments(source));
    for (let i = 0; i < tokens.length - 2; i++) {
      const name = tokens[i].value.replace(/^['"]|['"]$/g, '');
      if (!['styles', 'template'].includes(name) || tokens[i + 1].value !== ':') continue;
      let j = i + 2, depth = 0;
      if (tokens[j].value === '[') { depth = 1; j++; }
      else if (!tokens[j].string) continue;
      for (; j < tokens.length; j++) {
        const token = tokens[j];
        if (token.string) (name === 'styles' ? css : html)(token.value.slice(1, -1), token.start + 1);
        if (!depth) break;
        if (token.value === '[') depth++;
        if (token.value === ']' && --depth === 0) break;
      }
    }
  }
  findings.sort((a, b) => a.offset - b.offset);
  return findings.map(({ offset, ...finding }) => finding);
}

export async function scanDirectory(directory) {
  const issues = [];
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) issues.push(...await scanDirectory(path));
    else if (entry.isFile() && /\.(?:css|scss|sass|less|ts|html)$/.test(entry.name)) {
      issues.push(...scanSource(await readFile(path, 'utf8'), relative(process.cwd(), path)));
    }
  }
  return issues;
}

export async function runCheck(directory, write = console.log) {
  try {
    const issues = await scanDirectory(resolve(directory));
    for (const issue of issues) write(`${issue.path}:${issue.line} [${issue.rule}] ${issue.message}`);
    write(issues.length ? `FLAT: ${issues.length} vi phạm.` : 'FLAT: đạt.');
    return issues.length ? 1 : 0;
  } catch (error) {
    write(`FLAT: không kiểm được: ${error.message}`);
    return 1;
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  process.exitCode = await runCheck(process.argv[2] ?? 'apps/frontend/src');
}
