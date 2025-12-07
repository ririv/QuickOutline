package com.ririv.quickoutline.service.pdfpreview;

/**
 * Record representing an update for a single page's image.
 * Used to communicate changes from the backend to the frontend.
 *
 * @param pageIndex The 0-based index of the page that was updated.
 * @param version A unique identifier for this update (e.g., timestamp) to help frontend cache busting.
 * @param totalPages The total number of pages in the document after this update (contextual for frontend).
 * @param widthPt The width of the page in points.
 * @param heightPt The height of the page in points.
 */
public record ImagePageUpdate(int pageIndex, long version, int totalPages, float widthPt, float heightPt) {}
