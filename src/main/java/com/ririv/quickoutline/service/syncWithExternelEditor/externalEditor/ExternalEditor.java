package com.ririv.quickoutline.service.syncWithExternelEditor.externalEditor;

import java.util.function.Consumer;

public interface ExternalEditor {

    void launch(Consumer<String> operateAfterReturn) ;

}
