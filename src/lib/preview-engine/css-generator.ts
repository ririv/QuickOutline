import type { PageLayout } from '@/lib/types/page';

export function generatePageCss(header: any, footer: any, layout?: PageLayout) {
    const getContent = (val: string) => {
        if (!val) return '""';
        const parts = val.split('{p}');
        const escapedParts = parts.map(part => {
            if (part === '') return null;
            const escaped = part.replace(/\\/g, '\\').replace(/"/g, '\"');
            return `"${escaped}"`;
        });
        let cssContent = '';
        for (let i = 0; i < escapedParts.length; i++) {
            if (escapedParts[i]) cssContent += escapedParts[i];
            if (i < escapedParts.length - 1) {
                if (cssContent.length > 0) cssContent += ' ';
                cssContent += 'counter(page)';
                if (i < escapedParts.length - 1) cssContent += ' ';
            }
        }
        return cssContent || '""';
    };

    const headerBorder = `border-bottom: 1px solid ${header?.drawLine ? 'black' : 'transparent'}; padding-bottom: 5px;`;
    const footerBorder = `border-top: 1px solid ${footer?.drawLine ? 'black' : 'transparent'}; padding-top: 5px;`;

    // Layout values
    const sizeName = layout?.size || 'A4';
    const orientation = layout?.orientation || 'portrait';
    const mt = layout?.marginTop ?? 20;
    const mb = layout?.marginBottom ?? 20;
    const ml = layout?.marginLeft ?? 20;
    const mr = layout?.marginRight ?? 20;

    // Define standard sizes in mm (Portrait W x H)
    const sizes: Record<string, [string, string]> = {
        'A4': ['210mm', '297mm'],
        'A3': ['297mm', '420mm'],
        'Letter': ['215.9mm', '279.4mm'],
        'Legal': ['215.9mm', '355.6mm']
    };

    let width = sizes['A4'][0];
    let height = sizes['A4'][1];

    if (sizes[sizeName]) {
        if (orientation === 'landscape') {
            width = sizes[sizeName][1];
            height = sizes[sizeName][0];
        } else {
            width = sizes[sizeName][0];
            height = sizes[sizeName][1];
        }
    }

    return `
      @page {
          size: ${width} ${height};
          margin-top: ${mt}mm;
          margin-bottom: ${mb}mm;
          margin-left: ${ml}mm;
          margin-right: ${mr}mm;
          
          @top-left { 
              content: ${getContent(header?.left)};
              vertical-align: bottom;
              ${headerBorder} 
          }
          @top-center { 
              content: ${getContent(header?.center)}; 
              vertical-align: bottom;
              ${headerBorder} 
          }
          @top-right { 
              content: ${getContent(header?.right)}; 
              vertical-align: bottom;
              ${headerBorder} 
          }
          
          @bottom-left { 
              content: ${getContent(footer?.left)}; 
              vertical-align: top;
              ${footerBorder} 
          }
          @bottom-center { 
              content: ${getContent(footer?.center)}; 
              vertical-align: top;
              ${footerBorder} 
          }
          @bottom-right { 
              content: ${getContent(footer?.right)}; 
              vertical-align: top;
              ${footerBorder} 
          }
      }
    `;
}
