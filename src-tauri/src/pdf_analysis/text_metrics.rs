use pdfium_render::prelude::*;

/**
 * The "100% Replica" Text Analysis Engine.
 * Replicates reference implementation's behavior by querying the font's actual layout metrics.
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
     */
    fn get_actual_font_space_width(char_info: &PdfPageTextChar) -> f32 {
        // PDFium's PdfPageTextChar can provide its underlying TextObject.
        if let Ok(text_obj) = char_info.text_object() {
            // width() returns the actual physical width of the text object (the char).
            // unscaled_font_size() returns the font size before matrix transformation.
            if let Ok(width) = text_obj.width() {
                let font_size = text_obj.unscaled_font_size().value;
                if font_size > 0.0 {
                    // Ratio equivalent to reference implementation's font width calculation.
                    return width.value / font_size;
                }
            }
        }
        
        // Final fallback if metrics are unreadable
        0.278
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
