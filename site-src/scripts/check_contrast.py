import json
import sys

from playwright.sync_api import sync_playwright


def analyze(page, url):
    page.set_viewport_size({"width": 1440, "height": 2200})
    page.goto(url)
    return page.evaluate(
        """
() => {
  function parseColor(input) {
    const match = input.match(/rgba?\\(([^)]+)\\)/);
    if (!match) {
      return null;
    }
    const parts = match[1].split(',').map((part) => Number(part.trim()));
    return {
      r: parts[0],
      g: parts[1],
      b: parts[2],
      a: Number.isFinite(parts[3]) ? parts[3] : 1,
    };
  }

  function blend(top, bottom) {
    const alpha = top.a + bottom.a * (1 - top.a);
    if (alpha === 0) {
      return { r: 255, g: 255, b: 255, a: 0 };
    }
    return {
      r: (top.r * top.a + bottom.r * bottom.a * (1 - top.a)) / alpha,
      g: (top.g * top.a + bottom.g * bottom.a * (1 - top.a)) / alpha,
      b: (top.b * top.a + bottom.b * bottom.a * (1 - top.a)) / alpha,
      a: alpha,
    };
  }

  function resolveBackground(element) {
    let current = element;
    let background = { r: 255, g: 255, b: 255, a: 1 };
    while (current) {
      const color = parseColor(getComputedStyle(current).backgroundColor);
      if (color && color.a > 0) {
        background = blend(color, background);
        if (background.a >= 0.999) {
          break;
        }
      }
      current = current.parentElement;
    }
    return [background.r, background.g, background.b];
  }

  function toPath(element) {
    if (!element) {
      return '';
    }
    const parts = [];
    let current = element;
    while (current && parts.length < 4) {
      let part = current.tagName.toLowerCase();
      if (current.id) {
        part += `#${current.id}`;
      } else if (current.classList.length > 0) {
        part += '.' + Array.from(current.classList).slice(0, 2).join('.');
      }
      parts.unshift(part);
      current = current.parentElement;
    }
    return parts.join(' > ');
  }

  function luminance(rgb) {
    const srgb = rgb.map((channel) => {
      const value = channel / 255;
      return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
    });
    return 0.2126 * srgb[0] + 0.7152 * srgb[1] + 0.0722 * srgb[2];
  }

  function contrastRatio(foreground, background) {
    const l1 = luminance(foreground);
    const l2 = luminance(background);
    const lighter = Math.max(l1, l2);
    const darker = Math.min(l1, l2);
    return (lighter + 0.05) / (darker + 0.05);
  }

  const selectors = ['p', 'li', 'a', 'span', 'strong', 'button', 'h1', 'h2', 'h3', 'h4', 'code'];
  const elements = Array.from(document.querySelectorAll(selectors.join(',')));
  return elements
    .filter((element) => {
      const text = (element.textContent || '').trim().replace(/\\s+/g, ' ');
      if (!text || text.length < 2) {
        return false;
      }
      const style = getComputedStyle(element);
      if (style.visibility === 'hidden' || style.display === 'none' || Number(style.opacity) < 0.9) {
        return false;
      }
      const rect = element.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0;
    })
    .map((element) => {
      const style = getComputedStyle(element);
      const color = parseColor(style.color);
      if (!color) {
        return null;
      }
      const background = resolveBackground(element);
      const ratio = contrastRatio([color.r, color.g, color.b], background);
      const fontSize = Number.parseFloat(style.fontSize);
      const fontWeight = Number.parseInt(style.fontWeight, 10) || 400;
      const largeText = fontSize >= 24 || (fontSize >= 18.66 && fontWeight >= 700);
      const threshold = largeText ? 3 : 4.5;
      return {
        text: (element.textContent || '').trim().replace(/\\s+/g, ' ').slice(0, 120),
        ratio,
        threshold,
        color: style.color,
        background: `rgb(${background.map((value) => Math.round(value)).join(', ')})`,
        path: toPath(element),
        fontSize: style.fontSize,
        fontWeight: style.fontWeight,
      };
    })
    .filter(Boolean)
    .filter((item) => item.ratio < item.threshold)
    .sort((a, b) => a.ratio - b.ratio)
    .slice(0, 30);
}
"""
    )


def main():
    urls = sys.argv[1:]
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page()
        for url in urls:
            findings = analyze(page, url)
            print(json.dumps({"url": url, "findings": findings}, indent=2))
        browser.close()


if __name__ == "__main__":
    main()
