<script module lang="ts">
    import trashSvg from '@/assets/icons/trash.svg?raw';
    import deleteSvg from '@/assets/icons/delete-item.svg?raw';
    import plusSvg from '@/assets/icons/plus.svg?raw';
    import successSvg from '@/assets/icons/success.svg?raw';
    import pageOrientationSvg from '@/assets/icons/page-orientation.svg?raw';
    import githubSvg from '@/assets/icons/github.svg?raw';
    import xiaohongshuSvg from '@/assets/icons/xiaohongshu.svg?raw';
    import refreshSvg from '@/assets/icons/refresh.svg?raw';
    import folderOpenSvg from '@/assets/icons/folder-open.svg?raw';
    import offsetSvg from '@/assets/icons/offset.svg?raw';
    import insertPositionSvg from '@/assets/icons/insert-position.svg?raw';
    import pageSetupSvg from '@/assets/icons/page-setup.svg?raw';
    import headerFooterSvg from '@/assets/icons/header-footer.svg?raw';
    import numberSignSvg from '@/assets/icons/number-sign.svg?raw';
    import playSvg from '@/assets/icons/play.svg?raw';
    import arrowUpSvg from '@/assets/icons/arrow/arrow-up.svg?raw';
    import arrowDownSvg from '@/assets/icons/arrow/arrow-down.svg?raw';
    import arrowUpDownSvg from '@/assets/icons/arrow/arrow-up-down.svg?raw';
    import rulerSvg from '@/assets/icons/ruler.svg?raw';
    import addSiblingSvg from '@/assets/icons/add-sibling.svg?raw';
    import addChildSvg from '@/assets/icons/add-child.svg?raw';

    const icons = {
        'trash': trashSvg,
        'delete': deleteSvg,
        'add': plusSvg,
        'add-sibling': addSiblingSvg,
        'add-child': addChildSvg,
        'check': successSvg,
        'page-orientation': pageOrientationSvg,
        'github': githubSvg,
        'xiaohongshu': xiaohongshuSvg,
        'refresh': refreshSvg,
        'folder-open': folderOpenSvg,
        'offset': offsetSvg,
        'insert-position': insertPositionSvg,
        'page-setup': pageSetupSvg,
        'header-footer': headerFooterSvg,
        'number-sign': numberSignSvg,
        'play': playSvg,
        'arrow-up': arrowUpSvg,
        'arrow-down': arrowDownSvg,
        'arrow-up-down': arrowUpDownSvg,
        'ruler': rulerSvg
    };

    export type IconName = keyof typeof icons;
</script>

<script lang="ts">
    interface Props {
        name?: IconName;
        data?: string; // Support direct raw SVG string
        class?: string;
        style?: string;
        width?: string | number;
        height?: string | number;
        size?: string | number;
    }

    let { name, data, class: className = "", style = "", width, height, size }: Props = $props();

    const svgContent = $derived.by(() => {
        // Prefer 'data' prop, fallback to registry lookup by 'name'
        const raw = data || (name ? icons[name] : "");
        if (!raw) return "";
        
        if (typeof DOMParser === 'undefined') return raw;

        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(raw, "image/svg+xml");
            const svg = doc.querySelector('svg');
            
            if (svg) {
                // 1. Handle Class Name
                if (className) {
                    const existingClass = svg.getAttribute('class');
                    svg.setAttribute('class', existingClass ? `${existingClass} ${className}` : className);
                }

                // 2. Handle Width/Height
                const finalWidth = width ?? size;
                const finalHeight = height ?? size;
                
                if (finalWidth) svg.setAttribute('width', String(finalWidth));
                if (finalHeight) svg.setAttribute('height', String(finalHeight));

                // 2.1 Handle Style
                if (style) {
                    const existingStyle = svg.getAttribute('style');
                    svg.setAttribute('style', existingStyle ? `${existingStyle}; ${style}` : style);
                }

                // 3. Handle Color (fill/stroke="currentColor")
                // Helper to check if a color should be replaced by currentColor
                const shouldReplaceColor = (color: string | null) => {
                    if (!color || color === 'none' || color === 'transparent') return false;
                    // Always keep currentColor as is
                    if (color === 'currentColor') return false;
                    // Replace black variations, assume others are intentional specific colors
                    const blacks = ['#000', '#000000', 'black', 'rgb(0, 0, 0)'];
                    return blacks.includes(color.toLowerCase().replace(/\s/g, ''));
                };

                const rootFill = svg.getAttribute('fill');
                if (shouldReplaceColor(rootFill)) {
                    svg.setAttribute('fill', 'currentColor');
                } else if (!rootFill) {
                    // Default to currentColor if no fill is specified on root
                    svg.setAttribute('fill', 'currentColor');
                }

                const allElements = svg.querySelectorAll('*');
                allElements.forEach(el => {
                    const elFill = el.getAttribute('fill');
                    if (shouldReplaceColor(elFill)) {
                        el.setAttribute('fill', 'currentColor');
                    }

                    const elStroke = el.getAttribute('stroke');
                    if (shouldReplaceColor(elStroke)) {
                        el.setAttribute('stroke', 'currentColor');
                    }
                });

                return svg.outerHTML;
            }
        } catch (e) {
            console.error("Icon parsing failed:", e);
        }
        return raw;
    });
</script>

{@html svgContent}