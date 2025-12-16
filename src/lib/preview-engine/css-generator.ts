import type { PageLayout, HeaderFooterLayout } from '@/lib/types/page';
import { PAGE_SIZES_MM } from '@/lib/types/page';
import { css } from '@/lib/utils/tags';

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

    let widthNum = PAGE_SIZES_MM['A4'][0];
    let heightNum = PAGE_SIZES_MM['A4'][1];

    if (PAGE_SIZES_MM[sizeName]) {
        if (orientation === 'landscape') {
            widthNum = PAGE_SIZES_MM[sizeName][1];
            heightNum = PAGE_SIZES_MM[sizeName][0];
        } else {
            widthNum = PAGE_SIZES_MM[sizeName][0];
            heightNum = PAGE_SIZES_MM[sizeName][1];
        }
    }

    const width = `${widthNum}mm`;
    const height = `${heightNum}mm`;

    const headerBorder = header?.drawLine ? `border-bottom: 1px solid black; padding-bottom: ${headerPadding}pt;` : 'border-bottom: none; padding-bottom: 0;';
    const footerBorder = footer?.drawLine ? `border-top: 1px solid black; padding-top: ${footerPadding}pt;` : 'border-top: none; padding-top: 0;';

    return css`
      /* Running Elements Styles */
      /*
       * Use high-specificity selector (0,3,1) to reliably override Paged.js's default styles
       * for running elements (which often have specificity 0,3,0, e.g., .pagedjs_page ... > *).
       * This avoids using !important while ensuring 'display: flex' is applied.
       */
      div.print-header.print-header.print-header {
          position: running(headerRunning);
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          width: 100%;
          font-size: 10pt;
          font-family: serif; 
          ${headerBorder}
      }
      /*
       * Use high-specificity selector (0,3,1) to reliably override Paged.js's default styles
       * for running elements (which often have specificity 0,3,0, e.g., .pagedjs_page ... > *).
       * This avoids using !important while ensuring 'display: flex' is applied.
       */
      div.print-footer.print-footer.print-footer {
          position: running(footerRunning);
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          width: 100%;
          font-size: 10pt;
          font-family: serif;
          ${footerBorder}
      }
      
      .section-left { text-align: left; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .section-center { text-align: center; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .section-right { text-align: right; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      
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