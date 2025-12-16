// src/lib/utils/jsx.ts

// 1. 定义自闭合标签（不需要关闭标签的元素）
const voidTags = new Set([
    'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr'
]);

// 2. 属性处理：把对象转换成 key="value" 字符串
function renderProps(props: any): string {
    if (!props) return '';
    let str = '';
    for (const key in props) {
        const val = props[key];
        
        // 处理 style 对象: style={{ color: 'red', fontSize: '12px' }} -> style="color:red;font-size:12px"
        if (key === 'style' && typeof val === 'object') {
            const styleStr = Object.keys(val).map(k => {
                const cssKey = k.replace(/([A-Z])/g, '-$1').toLowerCase(); // camelCase to kebab-case
                return `${cssKey}:${val[k]}`;
            }).join(';');
            str += ` style="${styleStr}"`;
            continue;
        }

        if (val === true) {
            str += ` ${key}`; // 布尔属性，如 checked, disabled
        } else if (val !== false && val != null) {
            // 简单的转义，防止破坏 HTML 结构
            const sanitizedVal = String(val).replace(/"/g, '&quot;'); 
            str += ` ${key}="${sanitizedVal}"`;
        }
    }
    return str;
}

// 3. 核心工厂函数：JSX 编译的目标
export function createElement(tag: any, props: any, ...children: any[]): string {
    // 处理子元素：扁平化数组，过滤 null/undefined，转成字符串拼接
    const childStr = children
        .flat(Infinity)
        .map(child => {
            if (child == null || child === false) return '';
            return String(child);
        })
        .join('');

    // 情况 A: 如果是函数组件 <MyComponent />
    if (typeof tag === 'function') {
        return tag({ ...props, children: children.length === 1 ? children[0] : children });
    }

    // 情况 B: 如果是 Fragment <></>
    if (tag === Fragment) {
        return childStr;
    }

    // 情况 C: 普通 HTML 标签 <div>
    const attrs = renderProps(props);
    
    if (voidTags.has(tag)) {
        return `<${tag}${attrs} />`;
    }
    
    return `<${tag}${attrs}>${childStr}</${tag}>`;
}

// 4. Fragment 占位符 - 把它变成一个函数，直接返回子元素
export function Fragment(props: { children?: any }) {
    // 确保 children 扁平化并转为字符串
    return Array.isArray(props.children) 
        ? props.children.flat(Infinity).map(child => (child == null || child === false ? '' : String(child))).join('')
        : (props.children == null || props.children === false ? '' : String(props.children));
}
