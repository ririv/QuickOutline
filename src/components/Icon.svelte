<script lang="ts">
    import trashSvg from '@/assets/icons/trash.svg?raw';
    import deleteSvg from '@/assets/icons/delete-item.svg?raw';
    import plusSvg from '@/assets/icons/plus.svg?raw';
    import successSvg from '@/assets/icons/success.svg?raw';
    import pageOrientationSvg from '@/assets/icons/page-orientation.svg?raw'; // Import new SVG

    interface Props {
        name?: string;
        data?: string; // Support direct raw SVG string
        class?: string;
        width?: string | number;
        height?: string | number;
    }

    let { name, data, class: className = "", width, height }: Props = $props();

    const icons: Record<string, string> = {
        'trash': trashSvg,
        'delete': deleteSvg,
        'add': plusSvg,
        'check': successSvg,
        'page-orientation': pageOrientationSvg // Add new SVG to map
    };

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
                if (width) svg.setAttribute('width', String(width));
                if (height) svg.setAttribute('height', String(height));

                // 3. Handle Color (fill="currentColor")
                const rootFill = svg.getAttribute('fill');
                if (!rootFill || (rootFill !== 'none' && rootFill !== 'currentColor')) {
                    svg.setAttribute('fill', 'currentColor');
                }

                const allElements = svg.querySelectorAll('*');
                allElements.forEach(el => {
                    const elFill = el.getAttribute('fill');
                    if (elFill && elFill !== 'none' && elFill !== 'transparent') {
                        el.setAttribute('fill', 'currentColor');
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