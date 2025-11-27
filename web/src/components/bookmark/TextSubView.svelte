<script lang="ts">
    import StyledButton from '../controls/StyledButton.svelte';
    // Placeholder for TextSubView
</script>

<div class="text-subview-container">
    <div class="editor-area">
        <textarea placeholder="Enter bookmarks here..."></textarea>
    </div>
    <div class="sidebar">
        <StyledButton type="primary">VSCode</StyledButton>
        <StyledButton type="primary">Auto-Format</StyledButton>

        <div class="method-group">
            <label class="radio-button">
                <input type="radio" name="method" value="sequential" checked />
                <div class="radio">
                    <div class="dot"></div>
                </div>
                <span class="text">Sequential</span>
            </label>
            <label class="radio-button">
                <input type="radio" name="method" value="indent" />
                <div class="radio">
                    <div class="dot"></div>
                </div>
                <span class="text">Indent</span>
            </label>
        </div>
    </div>
</div>

<style>
    .text-subview-container {
        display: flex;
        height: 100%;
        width: 100%;
    }
    .editor-area {
        flex: 1;
        padding: 10px;
    }
    textarea {
        width: 100%;
        height: 100%;
        border: none;
        resize: none;
        outline: none;
        font-family: monospace;
        font-size: 14px;
        background-color: white; /* Match JavaFX */
        padding: 5px;
        box-sizing: border-box;
    }
    .sidebar {
        width: 150px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
        border-left: 1px solid #dfdfdf;
        background-color: white; /* Match JavaFX */
    }
    .method-group {
        margin-top: auto;
        font-size: 13px;
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    /* Custom Radio Button Styles (mimicking JavaFX BasicControls.css) */
    .radio-button {
        display: flex;
        align-items: center;
        cursor: pointer;
        user-select: none;
    }
    .radio-button input[type="radio"] {
        /* Hide the default radio input */
        position: absolute;
        opacity: 0;
        width: 0;
        height: 0;
    }
    .radio {
        width: 16px; /* Size of the outer circle */
        height: 16px;
        border: 1.5px solid var(--el-color-default-border);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        transition: all 0.2s ease-in-out;
    }
    .dot {
        width: 8px; /* Size of the inner dot */
        height: 8px;
        background-color: transparent;
        border-radius: 50%;
        transition: all 0.2s ease-in-out;
    }
    .radio-button input[type="radio"]:checked ~ .radio {
        border-color: var(--el-color-primary);
    }
    .radio-button input[type="radio"]:checked ~ .radio .dot {
        background-color: var(--el-color-primary);
    }
    .radio-button:hover .radio {
        border-color: var(--el-color-primary);
    }
    .radio-button:hover .text {
        color: var(--el-color-primary);
    }
    .radio-button .text {
        margin-left: 5px;
        color: var(--el-color-default-text);
        transition: color 0.2s ease-in-out;
    }
</style>
