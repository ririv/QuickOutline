package com.ririv.quickoutline.di

import com.ririv.quickoutline.event.AppEventBus
import com.ririv.quickoutline.pdfProcess.TocPageGenerator
import com.ririv.quickoutline.pdfProcess.itextImpl.iTextTocPageGenerator
import com.ririv.quickoutline.service.PdfOutlineService
import com.ririv.quickoutline.service.PdfPageLabelService
import com.ririv.quickoutline.service.PdfTocExtractorService
import com.ririv.quickoutline.service.PdfTocPageGeneratorService
import com.ririv.quickoutline.view.controls.MessageContainerState
import com.ririv.quickoutline.view.viewmodel.*
import org.koin.dsl.module

val appModule = module {
    single { MessageContainerState() }
    single { MainViewModel(get()) }
    single { PdfOutlineService() }
    single<TocPageGenerator> { iTextTocPageGenerator() }
    single { PdfTocPageGeneratorService(get()) }
    single { AppEventBus() }
    single { PdfTocExtractorService() }
    single { PdfPageLabelService() }
    single { BookmarkViewModel(get(), get(), get()) }
    factory { TocGeneratorViewModel(get(), get()) }
    factory { LeftPaneViewModel() }
    factory { PageLabelViewModel(get(), get(), get()) }
    factory { PdfPreviewViewModel(get()) }
    factory { ThumbnailViewModel(get(), get()) }
}