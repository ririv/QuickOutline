package com.ririv.quickoutline.di;

import com.ririv.quickoutline.view.TocGeneratorTabController;
import com.ririv.quickoutline.service.PdfTocPageGeneratorService;
import com.ririv.quickoutline.pdfProcess.TocPageGenerator;
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator;
import com.ririv.quickoutline.view.TreeTabController;
import com.ririv.quickoutline.view.LeftPaneController;
import com.ririv.quickoutline.view.PageLabelController;
import com.ririv.quickoutline.view.GetContentsPopupController;
import com.ririv.quickoutline.view.SetContentsPopupController;
import com.ririv.quickoutline.event.AppEventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.ririv.quickoutline.service.PdfOutlineService;
import com.ririv.quickoutline.state.CurrentFileState;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CurrentFileState.class).in(Scopes.SINGLETON);
        bind(PdfOutlineService.class).in(Scopes.SINGLETON);
        bind(PageLabelController.class).in(Scopes.SINGLETON);
        bind(LeftPaneController.class).in(Scopes.SINGLETON);
        bind(TreeTabController.class).in(Scopes.SINGLETON);
        bind(TocGeneratorTabController.class).in(Scopes.SINGLETON);
        bind(TocPageGenerator.class).to(iTextTocPageGenerator.class).in(Scopes.SINGLETON);
        bind(PdfTocPageGeneratorService.class).in(Scopes.SINGLETON);
        bind(GetContentsPopupController.class).in(Scopes.SINGLETON);
        bind(SetContentsPopupController.class).in(Scopes.SINGLETON);
        bind(AppEventBus.class).toInstance(AppEventBus.getInstance());
    }
}