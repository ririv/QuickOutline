use std::io::Write;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};

use anyhow::{Result, anyhow};
use tauri::{App, Manager};
use tauri_plugin_cli::{CliExt, Matches};

use crate::pdf_outline::io;
use crate::pdf_outline::model::{Bookmark, ViewScaleType};

pub fn run_if_needed(app: &mut App) -> Result<bool> {
    let matches = app
        .cli()
        .matches()
        .map_err(|err| anyhow!(err.to_string()))?;
    let Some(outline_matches) = matches
        .subcommand
        .as_deref()
        .filter(|subcommand| subcommand.name == "outline")
    else {
        return Ok(false);
    };

    let command = outline_matches
        .matches
        .subcommand
        .as_deref()
        .ok_or_else(|| anyhow!("Missing outline command."))?;

    let result = match command.name.as_str() {
        "import" => run_import(app, &command.matches),
        "export" => run_export(app, &command.matches),
        name => Err(anyhow!("Unsupported outline command: {name}")),
    };

    let exit_code = if let Err(err) = result {
        eprintln!("QuickOutline CLI failed: {err}");
        1
    } else {
        0
    };

    app.handle().exit(exit_code);
    Ok(true)
}

fn run_import(app: &App, matches: &Matches) -> Result<()> {
    let pdf_path = require_string_arg(matches, "pdf")?;
    let outline_path = require_string_arg(matches, "outline")?;
    let output_path = get_string_arg(matches, "output");
    let offset = parse_offset(matches)?;
    let method = get_string_arg(matches, "method").unwrap_or_else(|| "seq".to_string());
    let view_mode = parse_view_mode(get_string_arg(matches, "view-mode").as_deref())?;

    let outline_text = std::fs::read_to_string(&outline_path)
        .map_err(|err| anyhow!("Failed to read outline text {}: {err}", outline_path))?;
    let bookmark_root = parse_outline_text(app, &outline_text, &method)?;
    let output = output_path.as_deref().map(Path::new);

    io::set_outline_from_path(
        Path::new(&pdf_path),
        bookmark_root,
        output,
        offset,
        view_mode,
    )?;
    Ok(())
}

fn run_export(app: &App, matches: &Matches) -> Result<()> {
    let pdf_path = require_string_arg(matches, "pdf")?;
    let output_path = get_string_arg(matches, "output");
    let offset = parse_offset(matches)?;

    let outline = io::get_outline_from_path(Path::new(&pdf_path), offset)?;
    let output =
        io::resolve_outline_text_path(Path::new(&pdf_path), output_path.as_deref().map(Path::new));
    std::fs::write(&output, serialize_outline(app, &outline)?)
        .map_err(|err| anyhow!("Failed to write outline text {}: {err}", output.display()))?;
    Ok(())
}

fn get_string_arg(matches: &Matches, name: &str) -> Option<String> {
    matches
        .args
        .get(name)
        .and_then(|arg| arg.value.as_str())
        .map(str::trim)
        .filter(|value| !value.is_empty())
        .map(ToOwned::to_owned)
}

fn require_string_arg(matches: &Matches, name: &str) -> Result<String> {
    get_string_arg(matches, name).ok_or_else(|| anyhow!("Missing required option --{name}"))
}

fn parse_offset(matches: &Matches) -> Result<i32> {
    let Some(value) = get_string_arg(matches, "offset") else {
        return Ok(0);
    };
    value
        .parse::<i32>()
        .map_err(|_| anyhow!("Invalid --offset value: {value}"))
}

fn parse_view_mode(value: Option<&str>) -> Result<ViewScaleType> {
    match value.unwrap_or("none") {
        "none" => Ok(ViewScaleType::None),
        "fit-to-page" => Ok(ViewScaleType::FitToPage),
        "fit-to-width" => Ok(ViewScaleType::FitToWidth),
        "fit-to-height" => Ok(ViewScaleType::FitToHeight),
        "fit-to-box" => Ok(ViewScaleType::FitToBox),
        "actual-size" => Ok(ViewScaleType::ActualSize),
        value => Err(anyhow!("Invalid --view-mode value: {value}")),
    }
}

fn parse_outline_text(app: &App, text: &str, method: &str) -> Result<Bookmark> {
    let output = run_parser_bundle(app, "parse", &[("--method", method)], text)?;
    serde_json::from_str(&output).map_err(|err| anyhow!("Failed to parse outline JSON: {err}"))
}

fn serialize_outline(app: &App, root: &Bookmark) -> Result<String> {
    let input = serde_json::to_string(root)
        .map_err(|err| anyhow!("Failed to serialize outline JSON: {err}"))?;
    run_parser_bundle(app, "serialize", &[], &input)
}

fn run_parser_bundle(app: &App, mode: &str, args: &[(&str, &str)], input: &str) -> Result<String> {
    let bundle_path = parser_bundle_path(app)?;
    let mut command = Command::new("node");
    command.arg(&bundle_path).arg(mode);
    for (name, value) in args {
        command.arg(name).arg(value);
    }

    let mut child = command
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn()
        .map_err(|err| {
            anyhow!(
                "Failed to start system node for outline parser. Make sure node is available in PATH: {err}"
            )
        })?;

    let mut stdin = child
        .stdin
        .take()
        .ok_or_else(|| anyhow!("Failed to open outline parser stdin."))?;
    stdin
        .write_all(input.as_bytes())
        .map_err(|err| anyhow!("Failed to write outline parser input: {err}"))?;
    drop(stdin);

    let output = child
        .wait_with_output()
        .map_err(|err| anyhow!("Failed to read outline parser output: {err}"))?;

    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        return Err(anyhow!("Outline parser failed: {}", stderr.trim()));
    }

    String::from_utf8(output.stdout)
        .map_err(|err| anyhow!("Outline parser returned invalid UTF-8: {err}"))
}

fn parser_bundle_path(app: &App) -> Result<PathBuf> {
    let resource_path = app
        .path()
        .resource_dir()
        .map_err(|err| anyhow!("Failed to locate app resource directory: {err}"))?
        .join("resources")
        .join("outline-parser-cli.mjs");
    if resource_path.exists() {
        return Ok(resource_path);
    }

    let dev_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("resources")
        .join("outline-parser-cli.mjs");
    if dev_path.exists() {
        return Ok(dev_path);
    }

    Err(anyhow!(
        "Outline parser bundle not found. Run `npm run build:outline-parser-cli` first."
    ))
}
