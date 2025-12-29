<script>
    // 目录语法静态指南
</script>

<div class="guide-container">
    <h3>目录编辑指南</h3>
    
    <section>
        <h4>1. 基础格式</h4>
        <div class="code-block">标题 ... 页码</div>
        <p>示例：</p>
        <ul>
            <li><code>第一章 简介 ... 1</code></li>
            <li><code>附录 A ... iv</code></li>
        </ul>
        <p>注意：分隔符必须包含至少三个点 <code>...</code>。</p>
    </section>

    <section>
        <h4>2. 默认跳转逻辑（无 "|" 时）</h4>
        <ul>
            <li><strong>逻辑页码（纯数字，如 5）：</strong> 
                <br>跳转目标 = <code>5 + Offset</code>（如果设置了偏移量）。
                <br>若偏移量为 0，优先匹配名为 "5" 的页面标签，匹配失败则跳至物理第 5 页。
            </li>
            <li><strong>页面标签（非数字，如 iv）：</strong> 直接匹配名为 "iv" 的页面标签。不受 Offset 影响。</li>
        </ul>
    </section>

    <section>
        <h4>3. 自定义跳转目标 ("|")</h4>
        <div class="code-block">标题 ... 显示文字 | 实际目标</div>
        <p>当显示的文字与跳转目标不一致时使用。</p>
        <table class="syntax-table">
            <thead>
                <tr>
                    <th>目标语法</th>
                    <th>跳转说明</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><code>#10</code></td>
                    <td><strong>绝对物理页 10</strong>。忽略 Offset，最精确。</td>
                </tr>
                <tr>
                    <td><code>@iv</code></td>
                    <td><strong>强制匹配标签 "iv"</strong>。忽略 Offset。</td>
                </tr>
                <tr>
                    <td><code>toc:2</code></td>
                    <td>跳转到生成的目录自身的第 2 页。</td>
                </tr>
                <tr>
                    <td><code>1</code></td>
                    <td>逻辑页码。实际跳转：<code>1 + Offset</code>。</td>
                </tr>
            </tbody>
        </table>
    </section>
    
    <section>
        <h4>常见场景</h4>
        <ul>
            <li><strong>修正封面偏差：</strong> 在底部设置 Offset=2，输入 <code>... 1</code>。</li>
            <li><strong>手动强制跳转：</strong> <code>前言 ... 1 | #2</code></li>
        </ul>
    </section>
</div>

<style>
    .guide-container {
        padding: 10px 0;
        font-size: 13px;
        color: #444;
        max-height: 400px;
        overflow-y: auto;
        /* 移除边框和外边距，由父组件统一控制 */
        margin-top: 0;
    }

    h3 {
        margin: 0 0 10px 0;
        font-size: 15px;
        color: #222;
    }

    h4 {
        margin: 12px 0 6px 0;
        font-size: 14px;
        color: #333;
        border-bottom: 1px dashed #eee;
        padding-bottom: 4px;
    }

    p {
        margin: 4px 0;
    }

    ul {
        margin: 4px 0;
        padding-left: 20px;
    }

    li {
        margin-bottom: 4px;
    }

    .code-block {
        background: #f4f4f4;
        padding: 4px 8px;
        border-radius: 4px;
        font-family: monospace;
        margin: 4px 0;
        display: inline-block;
        border: 1px solid #e0e0e0;
    }

    code {
        background: #f4f4f4;
        padding: 2px 4px;
        border-radius: 3px;
        font-family: monospace;
        font-size: 12px;
        color: #c7254e;
    }

    .syntax-table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 8px;
        font-size: 12px;
    }

    .syntax-table th, .syntax-table td {
        border: 1px solid #ddd;
        padding: 6px;
        text-align: left;
    }

    .syntax-table th {
        background-color: #f9f9f9;
        font-weight: 600;
    }
</style>