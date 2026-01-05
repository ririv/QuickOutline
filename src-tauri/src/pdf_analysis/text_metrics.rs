use crate::pdf_analysis::models::AnalyzableChar;

/**
 * Text Analysis Metrics Helper.
 * Handles font metric calculations and coordinate transformations.
 */
pub struct TextMetrics;

impl TextMetrics {
    /**
     * Calculates the visual width of a space character.
     */
    pub fn calculate_visual_space_width(char_info: &AnalyzableChar) -> f32 {
        let unscaled_width = Self::get_actual_font_space_width(char_info);
        Self::apply_transformation_matrix_to_width(unscaled_width, char_info)
    }

    /**
     * Queries for the font's design-space width of a space.
     * NOTE: Currently returns 0.0, fallback logic is handled in processor.rs
     */
    fn get_actual_font_space_width(_char_info: &AnalyzableChar) -> f32 {
        0.0
    }

    /**
     * Applies the Transformation Matrix to convert design space to user space (Points).
     */
    fn apply_transformation_matrix_to_width(unscaled_width: f32, char_info: &AnalyzableChar) -> f32 {
        unscaled_width * char_info.font_size
    }
}