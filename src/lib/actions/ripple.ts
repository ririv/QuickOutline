export function ripple(node: HTMLElement, params: { color?: string } = {}) {
    const styleId = 'svelte-ripple-style';
    if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            .ripple-container {
                position: relative;
                overflow: hidden;
            }
            .ripple-effect {
                position: absolute;
                border-radius: 50%;
                transform: scale(0);
                animation: ripple-animation 600ms linear;
                pointer-events: none;
            }
            @keyframes ripple-animation {
                to {
                    transform: scale(4);
                    opacity: 0;
                }
            }
        `;
        document.head.appendChild(style);
    }

    node.classList.add('ripple-container');

    function handleMouseDown(e: MouseEvent) {
        const rect = node.getBoundingClientRect();
        const circle = document.createElement('span');
        const diameter = Math.max(rect.width, rect.height);
        const radius = diameter / 2;

        const x = e.clientX - rect.left - radius;
        const y = e.clientY - rect.top - radius;

        circle.style.width = circle.style.height = `${diameter}px`;
        circle.style.left = `${x}px`;
        circle.style.top = `${y}px`;
        circle.classList.add('ripple-effect');
        
        // Default color: semi-transparent black (good for light backgrounds)
        // Use 'rgba(255, 255, 255, 0.3)' for dark backgrounds
        const rippleColor = params.color || 'rgba(0, 0, 0, 0.1)'; 
        circle.style.backgroundColor = rippleColor;

        // Remove old ripples to prevent buildup, though multiple ripples are valid Material Design
        // Let's keep it simple and allow overlapping if user clicks fast? 
        // Material design allows multiple. But let's clean up after animation.
        
        node.appendChild(circle);

        setTimeout(() => {
            circle.remove();
        }, 600);
    }

    node.addEventListener('mousedown', handleMouseDown);

    return {
        destroy() {
            node.removeEventListener('mousedown', handleMouseDown);
        },
        update(newParams: { color?: string }) {
            params = newParams;
        }
    };
}
