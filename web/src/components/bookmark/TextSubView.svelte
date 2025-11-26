<script lang="ts">
    // Placeholder for TextSubView
</script>

<div class="text-subview-container">
    <div class="editor-area">
        <textarea placeholder="Enter bookmarks here..."></textarea>
    </div>
    <div class="sidebar">
        <button class="my-button plain-button-primary">VSCode</button>
        <button class="my-button plain-button-primary">Auto-Format</button>

        <div class="method-group">
            <label><input type="radio" name="method" checked /> Sequential</label>
            <label><input type="radio" name="method" /> Indent</label>
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
    }
    .sidebar {
        width: 150px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 20px;
        border-left: 1px solid #dfdfdf;
    }
    .method-group {
        margin-top: auto;
        font-size: 13px;
    }
</style>
