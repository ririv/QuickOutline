import type { PageLayout, HeaderFooterLayout } from '@/lib/types/page';

export function generatePageCss(header: any, footer: any, layout?: PageLayout, hfLayout?: HeaderFooterLayout) {
    // Layout values
    const sizeName = layout?.size || 'A4';
    const orientation = layout?.orientation || 'portrait';
    const mt = layout?.marginTop ?? 20;
    const mb = layout?.marginBottom ?? 20;
    const ml = layout?.marginLeft ?? 20;
    const mr = layout?.marginRight ?? 20;
    const headerDist = hfLayout?.headerDist ?? 10;
    const footerDist = hfLayout?.footerDist ?? 10;
    const headerPadding = hfLayout?.headerPadding ?? 1;
    const footerPadding = hfLayout?.footerPadding ?? 1;

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

    const headerBorder = header?.drawLine ? `border-bottom: 1px solid black; padding-bottom: ${headerPadding}pt;` : 'border-bottom: none; padding-bottom: 0;';
    const footerBorder = footer?.drawLine ? `border-top: 1px solid black; padding-top: ${footerPadding}pt;` : 'border-top: none; padding-top: 0;';

    return `
      /* Running Elements Styles */
      .print-header {
          position: running(headerRunning);
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          width: 100%;
          font-size: 10pt;
          font-family: serif; 
          ${headerBorder}
      }
      .print-footer {
          position: running(footerRunning);
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          width: 100%;
          font-size: 10pt;
          font-family: serif;
          ${footerBorder}
      }
      
      .section-left { text-align: left; flex: 1; }
      .section-center { text-align: center; flex: 1; }
      .section-right { text-align: right; flex: 1; }
      
      /* Page Number Injection */
      .page-num::after { content: counter(page); }

      @page {
          size: ${width} ${height};
          margin-top: ${mt}mm;
          margin-bottom: ${mb}mm;
          margin-left: ${ml}mm;
          margin-right: ${mr}mm;
          
          @top-center { 
              content: element(headerRunning); 
              vertical-align: top;
              padding-top: ${headerDist}mm;
              width: 100%;
          }
          
          @bottom-center { 
              content: element(footerRunning); 
              vertical-align: bottom;
              padding-bottom: ${footerDist}mm;
              width: 100%;
          }
          
          @top-left { content: none; }
          @top-right { content: none; }
          @bottom-left { content: none; }
          @bottom-right { content: none; }
      }
    `;
}