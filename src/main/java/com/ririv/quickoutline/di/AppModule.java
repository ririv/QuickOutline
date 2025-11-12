package com.ririv.quickoutline.di;

import com.ririv.quickoutline.service.MarkdownService;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.service.PdfTocExtractorService;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.state.BookmarkSettingsState;
import com.ririv.quickoutline.view.*;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.ririv.quickoutline.view.bookmarktab.*;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CurrentFileState.class).in(Scopes.SINGLETON);
        bind(PdfOutlineService.class).in(Scopes.SINGLETON);
        bind(PageLabelTabController.class).in(Scopes.SINGLETON);
        bind(LeftPaneController.class).in(Scopes.SINGLETON);
        bind(TreeSubViewController.class).in(Scopes.SINGLETON);
        bind(TocGeneratorTabController.class).in(Scopes.SINGLETON);
        bind(MarkdownTabController.class).in(Scopes.SINGLETON);
        bind(HelpController.class).in(Scopes.SINGLETON);
        bind(LeftPaneController.class).in(Scopes.SINGLETON);
        bind(MainController.class).in(Scopes.SINGLETON);
        bind(TocPageGenerator.class).to(iTextTocPageGenerator.class).in(Scopes.SINGLETON);
        bind(PdfTocPageGeneratorService.class).in(Scopes.SINGLETON);
        bind(MarkdownService.class).in(Scopes.SINGLETON);
        bind(GetContentsPopupController.class).in(Scopes.SINGLETON);
        bind(SetContentsPopupController.class).in(Scopes.SINGLETON);
        bind(AppEventBus.class).in(Scopes.SINGLETON);
        bind(PdfTocExtractorService.class).in(Scopes.SINGLETON);
        bind(BookmarkTabController.class).in(Scopes.SINGLETON);
        bind(BookmarkBottomPaneController.class).in(Scopes.SINGLETON);
        bind(BookmarkSettingsState.class).in(Scopes.SINGLETON);
        bind(SyncWithExternalEditorService.class).in(Scopes.SINGLETON);
        
    }

    
}