package com.ririv.quickoutline.view.event;

public class SwitchBookmarkViewEvent {
    public enum View {
        TEXT, TREE
    }

    private final View view;

    public SwitchBookmarkViewEvent(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }
}
