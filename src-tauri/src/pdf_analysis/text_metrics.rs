use pdfium_render::prelude::*;

/**
 * Text Analysis Metrics Helper.
 * Handles font metric calculations and coordinate transformations.
 */
pub struct TextMetrics;

impl TextMetrics {
    /**
     * Calculates the visual width of a space character.
     * Logic: Get the character's text object and query its unscaled width ratio.
     */
    pub fn calculate_visual_space_width(char_info: &PdfPageTextChar) -> f32 {
        let unscaled_width = Self::get_actual_font_space_width(char_info);
        Self::apply_transformation_matrix_to_width(unscaled_width, char_info)
    }

    /**
     * Queries PDFium for the font's design-space width of a space.
     * 
     * NOTE: This function is currently UNIMPLEMENTED / DEPRECATED.
     * 
     * Reason: The previous implementation incorrectly used `text_obj.width()`, which returns the width 
     * of the *current character* (e.g., 'A', '1', '.'), not the width of the space glyph for that font.
     * This caused severe over-segmentation when narrow characters were encountered.
     * 
     * Since `pdfium-render` does not expose an easy API to query the Font Dictionary for the specific width of the 'space' glyph,
     * we have moved the space width calculation logic to `processor.rs`. 
     * There, we use a robust "Page-Global Advance Width Inference" strategy, which statistically derives the 
     * space width from the actual character layout on the page.
     * 
     * This function now returns 0.0 to ensure any legacy callers fallback to the heuristic default.
     */
    fn get_actual_font_space_width(_char_info: &PdfPageTextChar) -> f32 {
        0.0
    }

    /**
     * Applies the 3x3 Transformation Matrix to convert design space to user space (Points).
     */
    fn apply_transformation_matrix_to_width(unscaled_width: f32, char_info: &PdfPageTextChar) -> f32 {
        if let Ok(matrix) = char_info.matrix() {
            let horizontal_scale = (matrix.a() * matrix.a() + matrix.b() * matrix.b()).sqrt();
            unscaled_width * horizontal_scale
        } else {
            unscaled_width * char_info.scaled_font_size().value
        }
    }
}
